package net.ds.trigamma.block;

import com.mojang.serialization.MapCodec;
import net.ds.trigamma.block.entity.PressBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PressBlock extends BaseEntityBlock {

    public static final EnumProperty<PressPart> PART = EnumProperty.create("part", PressPart.class);
    /** 0 = hammer up, up to 4 = hammer fully down. Purely visual - drives which model/texture is used. */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);

    public static final com.mojang.serialization.MapCodec<PressBlock> CODEC = simpleCodec(PressBlock::new);

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public PressBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, PressPart.LOWER).setValue(STAGE, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, STAGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // -----------------------------------------------------------------
    // Placement: place LOWER where clicked, UPPER directly above it
    // -----------------------------------------------------------------

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState().setValue(PART, PressPart.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(PART, PressPart.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        PressPart part = state.getValue(PART);
        if (direction.getAxis() == Direction.Axis.Y
                && (part == PressPart.LOWER) == (direction == Direction.UP)) {
            boolean otherHalfPresent = neighborState.is(this) && neighborState.getValue(PART) != part;
            return otherHalfPresent ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()) {
            PressPart part = state.getValue(PART);
            BlockPos otherPos = part == PressPart.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(PART) != part) {
                level.setBlock(otherPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && state.getValue(PART) == PressPart.LOWER) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PressBlockEntity press) {
                press.dropContents();
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // -----------------------------------------------------------------
    // Interaction routing
    // -----------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        PressBlockEntity be = getPressBE(level, pos, state);
        if (be == null) return InteractionResult.PASS;

        if (state.getValue(PART) == PressPart.UPPER) {
            be.crank(player);
        } else {
            be.tryExtract(player, player.isShiftKeyDown());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                                                  BlockPos pos, Player player, InteractionHand hand,
                                                                  BlockHitResult hit) {
        PressBlockEntity be = getPressBE(level, pos, state);
        if (be == null) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (stack.isEmpty()) {
            if (state.getValue(PART) == PressPart.UPPER) {
                be.crank(player);
            } else {
                be.tryExtract(player, player.isShiftKeyDown());
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (state.getValue(PART) == PressPart.UPPER) {
            be.crank(player);
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        boolean handled = be.insertItem(stack, player);
        return handled
                ? net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide)
                : net.minecraft.world.ItemInteractionResult.CONSUME;
    }

    @Nullable
    private PressBlockEntity getPressBE(Level level, BlockPos pos, BlockState state) {
        BlockPos lowerPos = state.getValue(PART) == PressPart.UPPER ? pos.below() : pos;
        BlockEntity be = level.getBlockEntity(lowerPos);
        return be instanceof PressBlockEntity press ? press : null;
    }

    // -----------------------------------------------------------------
    // Block entity plumbing
    // -----------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == PressPart.LOWER ? new PressBlockEntity(pos, state) : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
