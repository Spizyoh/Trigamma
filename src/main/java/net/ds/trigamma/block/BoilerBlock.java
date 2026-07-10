// net/ds/trigamma/block/BoilerBlock.java
package net.ds.trigamma.block;

import net.ds.trigamma.block.entity.BoilerBlockEntity;
import net.ds.trigamma.block.entity.BoilerShellBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

public class BoilerBlock extends Block implements EntityBlock {

    public BoilerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoilerBlockEntity(pos, state);
    }

    // Additions to BoilerBlock

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        if (!(level.getBlockEntity(pos) instanceof BoilerBlockEntity boiler)) return;

        boolean placed = boiler.tryPlaceShells(level, pos);
        if (!placed) {
            // Not enough clear space for the full 3x3x4 footprint - abort placement.
            level.destroyBlock(pos, false);
            if (placer instanceof Player player) {
                ItemStack refund = new ItemStack(this);
                if (!player.getInventory().add(refund)) {
                    player.drop(refund, false);
                }
                player.sendSystemMessage(Component.translatable("message.trigamma.boiler_no_space"));
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        if (level.isClientSide) return result;

        if (level.getBlockEntity(pos) instanceof BoilerBlockEntity boiler) {
            boiler.removeShells(level);
        }

        return result;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof BoilerBlockEntity boiler) {
                boiler.removeShells(level);
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void popMasterDrop(Level level, BlockPos masterPos, Player player) {
        if (player.isCreative()) return;
        net.minecraft.world.level.block.Block.popResource(level, masterPos,
                new net.minecraft.world.item.ItemStack(ModBlocks.BOILER.get()));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof BoilerBlockEntity boiler) {
                boiler.serverTick();
            }
        };
    }
}