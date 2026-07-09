package net.ds.trigamma.block.entity;

import net.ds.trigamma.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BoilerShellBlockEntity extends BlockEntity {

    private BlockPos masterPos;

    public BoilerShellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER_SHELL.get(), pos, state);
    }

    public void setMasterPos(BlockPos masterPos) {
        this.masterPos = masterPos;
        setChanged();
    }

    @Nullable
    public BlockPos getMasterPos() {
        return masterPos;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (masterPos != null) tag.putLong("MasterPos", masterPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("MasterPos")) masterPos = BlockPos.of(tag.getLong("MasterPos"));
    }
}