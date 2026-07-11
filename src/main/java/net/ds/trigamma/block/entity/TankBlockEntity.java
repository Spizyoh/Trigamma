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
import net.ds.trigamma.inventory.fluid.MatterRegistry;
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
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class TankBlockEntity extends BlockEntity implements IMatterBufferHolder, IPortableMachine, SyncableMachine {
    public static final int WIDTH_X = 5;
    public static final int DEPTH_Z = 3;
    public static final int HEIGHT_Y = 3;

    public static final int MIN_X = -2;
    public static final int MAX_X = 2;
    public static final int MIN_Z = -1;
    public static final int MAX_Z = 1;
    public static final int MIN_Y = 0;
    public static final int MAX_Y = 2;

    private static final int CAPACITY = 256000;

    private final MatterBuffer buffer = new MatterBuffer(CAPACITY);
    private final Set<BlockPos> shellOffsets = new HashSet<>();
    private IMatter lockedMatter;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TANK.get(), pos, state);
    }

    public MatterBuffer getBuffer() {
        return buffer;
    }

    public Optional<IMatter> getLockedMatter() {
        return Optional.ofNullable(lockedMatter);
    }

    public boolean setLockedMatter(IMatter matter) {
        if (!buffer.isEmpty() && buffer.getMatter().isPresent()
                && !buffer.getMatter().get().id().equals(matter.id())) {
            return false;
        }

        this.lockedMatter = matter;
        sync();
        return true;
    }

    public static Optional<TankBlockEntity> resolve(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof TankBlockEntity tank) {
            return Optional.of(tank);
        }

        if (be instanceof TankShellBlockEntity shell && shell.getMasterPos() != null) {
            if (level.getBlockEntity(shell.getMasterPos()) instanceof TankBlockEntity tank) {
                return Optional.of(tank);
            }
        }

        if (be instanceof MachinePortBlockEntity port && port.getMasterPos() != null) {
            if (level.getBlockEntity(port.getMasterPos()) instanceof TankBlockEntity tank) {
                return Optional.of(tank);
            }
        }

        return Optional.empty();
    }

    @Override
    public DynamicPortConfig getPortConfig(BlockPos portPos) {
        BlockPos offset = portPos.subtract(getBlockPos());

        if (!isTankPortOffset(offset)) {
            return new DynamicPortConfig(PortKind.MATTER, PortIO.BOTH, null);
        }

        return new DynamicPortConfig(
                PortKind.MATTER,
                PortIO.BOTH,
                lockedMatter == null ? null : lockedMatter.phase()
        );
    }

    @Override
    public Optional<IMatterBuffer> getBufferForPort(BlockPos portPos, DynamicPortConfig config) {
        BlockPos offset = portPos.subtract(getBlockPos());
        return isTankPortOffset(offset) ? Optional.of(buffer) : Optional.empty();
    }

    private boolean isTankPortOffset(BlockPos offset) {
        int dx = offset.getX();
        int dy = offset.getY();
        int dz = offset.getZ();

        return dy == 0 && (
                // left side
                (dx == -2 && dz == -1) ||
                        (dx == -2 && dz == 1) ||

                        // right side
                        (dx == 2 && dz == -1) ||
                        (dx == 2 && dz == 1)
        );
    }

    public static List<BlockPos> getShellOffsets() {
        List<BlockPos> offsets = new ArrayList<>();

        for (int dx = MIN_X; dx <= MAX_X; dx++) {
            for (int dz = MIN_Z; dz <= MAX_Z; dz++) {
                for (int dy = MIN_Y; dy <= MAX_Y; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    offsets.add(new BlockPos(dx, dy, dz));
                }
            }
        }

        return offsets;
    }

    public boolean tryPlaceShells(Level level, BlockPos masterPos) {
        for (BlockPos offset : getShellOffsets()) {
            BlockPos target = masterPos.offset(offset);
            BlockState existing = level.getBlockState(target);

            if (!existing.isAir() && !existing.canBeReplaced()) {
                return false;
            }
        }

        shellOffsets.clear();

        BlockState shellState = ModBlocks.TANK_SHELL.get().defaultBlockState();
        BlockState portState = ModBlocks.MACHINE_PORT.get().defaultBlockState();

        for (BlockPos offset : getShellOffsets()) {
            BlockPos target = masterPos.offset(offset);
            BlockState stateToPlace = isTankPortOffset(offset) ? portState : shellState;

            level.setBlock(target, stateToPlace, Block.UPDATE_ALL);

            BlockEntity placedBe = level.getBlockEntity(target);

            if (placedBe instanceof TankShellBlockEntity shellBe) {
                shellBe.setMasterPos(masterPos);
            } else if (placedBe instanceof MachinePortBlockEntity portBe) {
                portBe.configure(masterPos);
            }

            shellOffsets.add(offset);
        }

        sync();
        return true;
    }

    public void removeShells(Level level) {
        for (BlockPos offset : getShellOffsets()) {
            BlockPos target = getBlockPos().offset(offset);
            BlockState state = level.getBlockState(target);

            if (state.is(ModBlocks.TANK_SHELL.get()) || state.is(ModBlocks.MACHINE_PORT.get())) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        shellOffsets.clear();
    }

    @Override
    public List<BufferSlot> getDisplayBuffers() {
        return List.of(new BufferSlot(Component.translatable("buffer.trigamma.tank"), buffer));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put("Buffer", buffer.save());

        if (lockedMatter != null) {
            tag.putString("LockedMatter", lockedMatter.id().toString());
        }

        tag.putLongArray("ShellOffsets", shellOffsets.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("Buffer")) {
            buffer.load(tag.getCompound("Buffer"));
        }

        if (tag.contains("LockedMatter")) {
            lockedMatter = MatterRegistry.get(ResourceLocation.parse(tag.getString("LockedMatter"))).orElse(null);
        }

        shellOffsets.clear();
        for (long packed : tag.getLongArray("ShellOffsets")) {
            shellOffsets.add(BlockPos.of(packed));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void sync() {
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }
}