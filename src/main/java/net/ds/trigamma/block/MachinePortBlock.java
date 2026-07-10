package net.ds.trigamma.block;

import com.mojang.serialization.MapCodec;
import net.ds.trigamma.block.entity.MachinePortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable, invisible port block used by any multiblock/pseudo-multiblock to expose
 * IMatterHandler connections for Universal Matter Ducts. Register one shared instance of
 * this and reuse it across every machine that adopts the port system - the actual
 * behavior (which buffer it talks to, input vs output, phase filter) all lives in the
 * per-instance MachinePortBlockEntity, configured from a PortSpec at placement time.
 * <p>
 * Visually and collision-wise this behaves exactly like a shell block: invisible model,
 * default (solid) collision shape, and breaking it tears down the whole structure.
 * <p>
 * TODO: Pneumatic Tubes don't exist yet - once added, this same block can serve as a
 * pneumatic port too, keyed off MachinePortBlockEntity#getPortKind().
 */
public class MachinePortBlock extends Block implements EntityBlock {
    public static final MapCodec<MachinePortBlock> CODEC = simpleCodec(MachinePortBlock::new);

    public MachinePortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachinePortBlockEntity(pos, state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        if (level.isClientSide) return result;

        if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port) {
            BlockPos masterPos = port.getMasterPos();
            if (masterPos != null && !level.getBlockState(masterPos).isAir()) {
                // Breaking any part of the structure breaks the whole thing - the master's
                // own loot table/removal logic handles drops and cleaning up the rest.
                level.destroyBlock(masterPos, !player.isCreative());
            }
        }
        return result;
    }
}