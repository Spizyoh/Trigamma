package net.ds.trigamma.block;

import net.ds.trigamma.block.entity.BoilerShellBlockEntity;
import net.ds.trigamma.block.entity.TankShellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TankShellBlock extends Block implements EntityBlock {

    public TankShellBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankShellBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The master's BER draws the entire visual model; shells stay invisible
        // but keep full solid collision (the default cube shape), so players
        // can't walk through or build inside the boiler's footprint.
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        if (level.isClientSide) return result;

        if (level.getBlockEntity(pos) instanceof TankShellBlockEntity shell) {
            BlockPos masterPos = shell.getMasterPos();
            if (masterPos != null && level.getBlockState(masterPos).getBlock() instanceof TankBlock) {
                // Breaking any part of the shell breaks the whole structure.
                // The master's own loot table handles the single item drop.
                level.destroyBlock(masterPos, !player.isCreative());
            }
        }
        return result;
    }
}