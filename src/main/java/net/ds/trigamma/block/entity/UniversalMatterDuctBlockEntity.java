package net.ds.trigamma.block.entity;

import net.ds.trigamma.inventory.fluid.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Optional;

public class UniversalMatterDuctBlockEntity extends BlockEntity {
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<UniversalMatterDuctBlockEntity>> TYPE =
            net.ds.trigamma.block.ModBlockEntities.UNIVERSAL_MATTER_DUCT;

    public static final int BUCKET_CAPACITY = 1000;

    private final PipeMatterTank tank = new PipeMatterTank(BUCKET_CAPACITY);
    private IMatter filterMatter = null;

    public UniversalMatterDuctBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, UniversalMatterDuctBlockEntity duct) {
        if (level.isClientSide) {
            return;
        }

        MatterNetwork network = duct.getNetwork();
        if (!pos.equals(network.leader())) {
            return;
        }

        network.tick(level);
    }

    public MatterNetwork getNetwork() {
        return MatterNetworkScanner.getNetwork(this.level, this.worldPosition);
    }

    public PipeMatterTank getTank() {
        return this.tank;
    }

    public Optional<IMatter> getFilterMatter() {
        if (this.filterMatter == null) {
            return this.getTank().getCurrentMatter();
        }
        return Optional.of(this.filterMatter);
    }

    public void setFilterMatter(IMatter matter) {
        this.filterMatter = matter;
        this.setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        MatterNetworkScanner.invalidate(this.level, this.worldPosition);
    }

    @Override
    public void setRemoved() {
        MatterNetworkScanner.invalidate(this.level, this.worldPosition);
        super.setRemoved();
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

        this.filterMatter = null;
        if (tag.contains("FilterMatterId")) {
            ResourceLocation id = ResourceLocation.parse(tag.getString("FilterMatterId"));
            MatterRegistry.get(id).ifPresent(matter -> this.filterMatter = matter);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null) {
            this.requestModelDataUpdate();

            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, registries);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.loadAdditional(tag, registries);

        if (this.level != null && this.level.isClientSide) {
            this.requestModelDataUpdate();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
}