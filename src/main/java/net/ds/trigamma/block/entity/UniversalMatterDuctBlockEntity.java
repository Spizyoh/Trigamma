package net.ds.trigamma.block.entity;

import net.ds.trigamma.inventory.fluid.IMatterHandler;
import net.ds.trigamma.inventory.fluid.MatterCapabilities;
import net.ds.trigamma.inventory.fluid.MatterPhase;
import net.ds.trigamma.inventory.fluid.PipeMatterTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

public class UniversalMatterDuctBlockEntity extends BlockEntity {
    // Registry Holder reference (you'll point this to your registration class)
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<UniversalMatterDuctBlockEntity>> TYPE =
            net.ds.trigamma.block.ModBlockEntities.UNIVERSAL_MATTER_DUCT;

    private static final int BUCKET_CAPACITY = 1000; // 1 Bucket = 1000 mB
    private final PipeMatterTank tank = new PipeMatterTank(BUCKET_CAPACITY);

    public UniversalMatterDuctBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE.get(), pos, state);
    }

    /**
     * Logic loop running 20 times a second on the server.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, UniversalMatterDuctBlockEntity duct) {
        if (duct.tank.isEmpty()) return;

        // Determine the distribution order based on what phase we are holding
        Direction[] priorityOrder = getDirectionPriority(duct.tank.getPhase());

        for (Direction direction : priorityOrder) {
            if (duct.tank.getAmount() <= 0) break;

            BlockPos targetPos = pos.relative(direction);
            // Neighbor can now be another duct (PipeMatterTank) OR a MachinePortBlockEntity -
            // both implement IMatterHandler, so this works for either transparently.
            IMatterHandler neighborTank = level.getCapability(MatterCapabilities.MATTER_HANDLER, targetPos, direction.getOpposite());

            if (neighborTank != null) {
                int ourAmount = duct.tank.getAmount();
                int neighborAmount = neighborTank.getAmount();

                // Cellular Rule Modification:
                // For horizontal movements, we use classic equalization (balance out evenly).
                // For vertical movements matching the natural flow (fluid down, gas up), we aggressively push ALL we can.
                boolean isNaturalVerticalFlow = (duct.tank.getPhase() == MatterPhase.FLUID && direction == Direction.DOWN) ||
                        (duct.tank.getPhase() == MatterPhase.GAS && direction == Direction.UP);

                int pushAmount;
                if (isNaturalVerticalFlow) {
                    // Pour as much as possible into the lower pipe (fluid) or upper pipe (gas)
                    pushAmount = Math.min(ourAmount, neighborTank.getCapacity() - neighborAmount);
                } else {
                    // Horizontal balancing or resisting natural flow (fluid climbing, gas sinking):
                    // Only push if we have more than the neighbor, and only push half the difference.
                    if (ourAmount > neighborAmount) {
                        pushAmount = (ourAmount - neighborAmount) / 2;
                    } else {
                        pushAmount = 0;
                    }
                }

                if (pushAmount > 0) {
                    duct.tank.getCurrentMatter().ifPresent(matter -> {
                        int accepted = neighborTank.fill(matter, pushAmount, false);
                        if (accepted > 0) {
                            duct.tank.drain(accepted, false);
                            duct.setChanged();

                            BlockEntity neighborBe = level.getBlockEntity(targetPos);
                            if (neighborBe != null) {
                                neighborBe.setChanged();
                            }
                        }
                    });
                }
            }
        }
    }

    /**
     * Helper method to sort direction scanning priority based on matter phase.
     */
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private static Direction[] getDirectionPriority(MatterPhase phase) {
        if (phase == MatterPhase.GAS) {
            return new Direction[]{
                    Direction.NORTH,
                    Direction.SOUTH,
                    Direction.EAST,
                    Direction.WEST,
                    Direction.UP,
                    Direction.DOWN
            };
        } else {
            return new Direction[]{
                    Direction.NORTH,
                    Direction.SOUTH,
                    Direction.EAST,
                    Direction.WEST,
                    Direction.DOWN,
                    Direction.UP
            };
        }
    }

    // Expose our tank capability so other ducts/blocks can find it
    public PipeMatterTank getTank() {
        return this.tank;
    }

    // --- Serialization (Saving/Loading) ---

    // Keep track of what this pipe is locked to, even when empty
    private net.ds.trigamma.inventory.fluid.IMatter filterMatter = null;

    public java.util.Optional<net.ds.trigamma.inventory.fluid.IMatter> getFilterMatter() {
        // Fallback to current tank matter if filter isn't explicitly set yet
        if (this.filterMatter == null) {
            return this.getTank().getCurrentMatter();
        }
        return java.util.Optional.of(this.filterMatter);
    }

    public void setFilterMatter(net.ds.trigamma.inventory.fluid.IMatter matter) {
        this.filterMatter = matter;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (this.getFilterMatter().isPresent()) {
            tag.putString("FilterMatterId", this.getFilterMatter().get().id().toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("FilterMatterId")) {
            ResourceLocation id = ResourceLocation.parse(tag.getString("FilterMatterId"));
            net.ds.trigamma.inventory.fluid.MatterRegistry.get(id).ifPresent(matter -> {
                this.filterMatter = matter;
            });
        }
    }

    // --- SERVER TO CLIENT DATA SYNCING ---

    /**
     * Outbound packet data from the server.
     */
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Grabs the tag data to send over the network mesh.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    /**
     * Tells the client to visually re-render the block when it receives an update packet.
     */
    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null) {
            // 1. Tell NeoForge to refresh the block's internal model tracking data
            this.requestModelDataUpdate();

            // 2. Force a packet transmission to clients and trigger a client-side chunk mesh redraw
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, registries);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        // This forces the client-side block entity to read the NBT packet data
        this.loadAdditional(tag, registries);

        // This forces the chunk to re-render visually on the client thread instantly
        if (this.level != null && this.level.isClientSide) {
            this.requestModelDataUpdate();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
}