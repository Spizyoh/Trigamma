// net/ds/trigamma/block/entity/BoilerBlockEntity.java
package net.ds.trigamma.block.entity;

import net.ds.trigamma.block.ModBlockEntities;
import net.ds.trigamma.block.ModBlocks;
import net.ds.trigamma.inventory.fluid.IMatter;
import net.ds.trigamma.inventory.fluid.MatterBuffer;
import net.ds.trigamma.inventory.recipes.BoilerRecipe;
import net.ds.trigamma.inventory.recipes.BoilerRecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class BoilerBlockEntity extends BlockEntity implements IMatterBufferHolder {

    private static final int INPUT_CAPACITY = 4000;
    private static final int OUTPUT_CAPACITY = 4000;

    public static final int RADIUS = 1; // 3 wide = center ± 1
    public static final int HEIGHT = 4;

    public static List<BlockPos> getShellOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = 0; dy < HEIGHT; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue; // master's own position
                    offsets.add(new BlockPos(dx, dy, dz));
                }
            }
        }
        return offsets;
    }

    private final MatterBuffer inputBuffer = new MatterBuffer(INPUT_CAPACITY);
    private final MatterBuffer outputBuffer = new MatterBuffer(OUTPUT_CAPACITY);

    private final Set<BlockPos> shellOffsets = new HashSet<>();

    private BoilerRecipe currentRecipe;

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER.get(), pos, state);
    }

    public MatterBuffer getInputBuffer() {
        return inputBuffer;
    }

    public MatterBuffer getOutputBuffer() {
        return outputBuffer;
    }

    public Optional<BoilerRecipe> getCurrentRecipe() {
        return Optional.ofNullable(currentRecipe);
    }

    /**
     * Called by the Identifier Tablet. Looks up the recipe whose input matches
     * the given matter and assigns it as the boiler's active recipe.
     * @return true if a matching recipe was found and set.
     */
    public boolean setRecipeFromInput(IMatter matter) {
        Optional<BoilerRecipe> recipe = BoilerRecipeRegistry.getByInput(matter.id());
        recipe.ifPresent(r -> this.currentRecipe = r);
        setChanged();
        return recipe.isPresent();
    }

    // Addition to BoilerBlockEntity
    public static Optional<BoilerBlockEntity> resolve(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BoilerBlockEntity master) {
            return Optional.of(master);
        }
        if (be instanceof BoilerShellBlockEntity shell && shell.getMasterPos() != null) {
            if (level.getBlockEntity(shell.getMasterPos()) instanceof BoilerBlockEntity master) {
                return Optional.of(master);
            }
        }
        return Optional.empty();
    }

    public void serverTick() {
        // TODO: once Thermal Units exist, gate processing here:
        //   - require currentRecipe != null
        //   - require inputBuffer has >= currentRecipe.inputAmount() of the right matter
        //   - require outputBuffer has room for currentRecipe.outputAmount()
        //   - require enough TU accumulated / consume TU per tick
        //   - on completion: inputBuffer.drain(...), outputBuffer.fill(...)
    }

    @Override
    public List<BufferSlot> getDisplayBuffers() {
        return List.of(
                new BufferSlot(Component.translatable("buffer.trigamma.input"), inputBuffer),
                new BufferSlot(Component.translatable("buffer.trigamma.output"), outputBuffer)
        );
    }

    public boolean tryPlaceShells(Level level, BlockPos masterPos) {
        List<BlockPos> offsets = getShellOffsets();

        // First pass: verify every position is free before touching anything.
        for (BlockPos offset : offsets) {
            BlockPos target = masterPos.offset(offset);
            BlockState existing = level.getBlockState(target);
            if (!existing.isAir() && !existing.canBeReplaced()) {
                return false;
            }
        }

        // Second pass: actually place the shell blocks.
        shellOffsets.clear();
        for (BlockPos offset : offsets) {
            BlockPos target = masterPos.offset(offset);
            level.setBlock(target, ModBlocks.BOILER_SHELL.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(target) instanceof BoilerShellBlockEntity shellBe) {
                shellBe.setMasterPos(masterPos);
            }
            shellOffsets.add(offset);
        }
        setChanged();
        return true;
    }

    public void removeShells(Level level) {
        BlockPos masterPos = this.getBlockPos();
        for (BlockPos offset : shellOffsets) {
            BlockPos target = masterPos.offset(offset);
            if (level.getBlockState(target).is(ModBlocks.BOILER_SHELL.get())) {
                level.setBlock(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        shellOffsets.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputBuffer", inputBuffer.save());
        tag.put("OutputBuffer", outputBuffer.save());

        long[] offsetLongs = shellOffsets.stream().mapToLong(BlockPos::asLong).toArray();
        tag.putLongArray("ShellOffsets", offsetLongs);

        if (currentRecipe != null) {
            tag.putString("RecipeInput", currentRecipe.input().id().toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("InputBuffer")) inputBuffer.load(tag.getCompound("InputBuffer"));
        if (tag.contains("OutputBuffer")) outputBuffer.load(tag.getCompound("OutputBuffer"));

        shellOffsets.clear();
        for (long l : tag.getLongArray("ShellOffsets")) {
            shellOffsets.add(BlockPos.of(l));
        }
        if (tag.contains("RecipeInput")) {
            ResourceLocation inputId = ResourceLocation.parse(tag.getString("RecipeInput"));
            BoilerRecipeRegistry.getByInput(inputId).ifPresent(r -> this.currentRecipe = r);
        }
    }
}