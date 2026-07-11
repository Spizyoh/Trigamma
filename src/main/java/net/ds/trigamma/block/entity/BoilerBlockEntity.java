// net/ds/trigamma/block/entity/BoilerBlockEntity.java
package net.ds.trigamma.block.entity;

import net.ds.trigamma.block.ModBlockEntities;
import net.ds.trigamma.block.ModBlocks;
import net.ds.trigamma.block.port.DynamicPortConfig;
import net.ds.trigamma.block.port.IPortableMachine;
import net.ds.trigamma.block.port.PortIO;
import net.ds.trigamma.block.port.PortKind;
import net.ds.trigamma.inventory.fluid.IMatter;
import net.ds.trigamma.inventory.fluid.IMatterBuffer;
import net.ds.trigamma.inventory.fluid.MatterBuffer;
import net.ds.trigamma.inventory.fluid.MatterPhase;
import net.ds.trigamma.inventory.recipes.BoilerRecipe;
import net.ds.trigamma.inventory.recipes.BoilerRecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class BoilerBlockEntity extends BlockEntity implements IMatterBufferHolder, IPortableMachine, SyncableMachine {

    private static final int INPUT_CAPACITY = 16000;
    private static final int OUTPUT_CAPACITY = 16000;

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

    @Override
    public DynamicPortConfig getPortConfig(BlockPos portPos) {
        BlockPos offset = toRelative(portPos);

        if (currentRecipe != null) {
            MatterPhase phase = currentRecipe.output().phase();

            if (isTopPortOffset(offset)) {
                return new DynamicPortConfig(
                        PortKind.MATTER,
                        PortIO.OUTPUT,
                        phase
                );
            }
        }
        if (isTopPortOffset(offset)) {
            return new DynamicPortConfig(
                    PortKind.MATTER,
                    PortIO.OUTPUT,
                    null
            );
        }
        return DynamicPortConfig.OMNI_MATTER;
    }

    @Override
    public Optional<IMatterBuffer> getBufferForPort(
            BlockPos portPos,
            DynamicPortConfig config) {
        BlockPos offset = toRelative(portPos);

        if (isTopPortOffset(offset)) {
            return Optional.of(outputBuffer);
        }
        if (isInputPortOffset(offset)) {
            return Optional.of(inputBuffer);
        }
        return Optional.empty();
    }

    private boolean isTopPortOffset(BlockPos offset) {
        // Relative check: The top port is located exactly at the center of the roof layer (dy == 3)
        return offset.getY() == 3 && offset.getX() == 0 && offset.getZ() == 0;
    }

    private BlockPos toRelative(BlockPos worldPos) {
        return worldPos.subtract(getBlockPos());
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
        System.out.println(
                "SERVER BUFFER: "
                        + inputBuffer.getAmount()
                        + " "
                        + inputBuffer.getMatter()
        );

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

        // 1. Pre-flight check: ensure the layout footprint is clear
        for (BlockPos offset : offsets) {
            BlockPos target = masterPos.offset(offset);

            // Safety check: Skip checking the master position itself if it slipped in
            if (target.equals(masterPos)) continue;

            BlockState existing = level.getBlockState(target);
            if (!existing.isAir() && !existing.canBeReplaced()) {
                // DEBUG LINE: Uncomment this to see exactly which block is blocking your multi-block
                System.out.println("Boiler placement blocked at: " + target + " by " + existing.getBlock());
                return false;
            }
        }

        shellOffsets.clear();

        // Get your default block states
        BlockState shellState = ModBlocks.BOILER_SHELL.get().defaultBlockState();
        BlockState portState = ModBlocks.MACHINE_PORT.get().defaultBlockState();

        // 2. Structural Placement Loop
        for (BlockPos offset : offsets) {
            BlockPos target = masterPos.offset(offset);

            boolean isPort =
                    isTopPortOffset(offset)
                            || isInputPortOffset(offset);

            BlockState stateToPlace =
                    isPort
                            ? portState
                            : shellState;

            level.setBlock(target, stateToPlace, Block.UPDATE_ALL);

            // Configure the placed block entity to recognize its structural master
            BlockEntity placedBe = level.getBlockEntity(target);
            if (placedBe instanceof BoilerShellBlockEntity shellBe) {
                shellBe.setMasterPos(masterPos);
                level.sendBlockUpdated(target, stateToPlace, stateToPlace, Block.UPDATE_ALL);
            } else if (placedBe instanceof MachinePortBlockEntity portBe) {
                // Configures our new smart dynamic port back-link!
                portBe.configure(masterPos);
                level.sendBlockUpdated(target, stateToPlace, stateToPlace, Block.UPDATE_ALL);
            }

            shellOffsets.add(offset);
        }

        setChanged();
        return true;
    }

    public void removeShells(Level level) {
        BlockPos masterPos = this.getBlockPos();

        for (BlockPos offset : getShellOffsets()) {
            BlockPos target = masterPos.offset(offset);
            BlockState currentState = level.getBlockState(target);

            // Clean up both standard shells and machine ports tied to this multiblock structure
            if (currentState.is(ModBlocks.BOILER_SHELL.get()) || currentState.is(ModBlocks.MACHINE_PORT.get())) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        shellOffsets.clear();
    }

    /**
     * Determines which bottom offsets should be exposed as fluid inputs.
     * Matches the core positions excluded from getShellOffsets layout.
     */
    private boolean isInputPortOffset(BlockPos offset) {
        int dx = offset.getX();
        int dy = offset.getY();
        int dz = offset.getZ();

        // Base ring plumbing connections (dy == 0)
        // Matches (0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0) relative to master
        return dy == 0 && (
                (dx == 1 && dz == 0) || (dx == -1 && dz == 0) ||
                        (dx == 0 && dz == 1) || (dx == 0 && dz == -1)
        );
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
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

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void sync() {
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_ALL
            );
        }
    }
}