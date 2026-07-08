package net.ds.trigamma.block;

import com.mojang.serialization.MapCodec;
import net.ds.trigamma.block.entity.UniversalMatterDuctBlockEntity;
import net.ds.trigamma.inventory.fluid.MatterCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class UniversalMatterDuctBlock extends BaseEntityBlock {
    public static final MapCodec<UniversalMatterDuctBlock> CODEC = simpleCodec(UniversalMatterDuctBlock::new);

    // 1. Define connection properties for all 6 sides
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public UniversalMatterDuctBlock(Properties properties) {
        super(properties);
        // Default state is unconnected on all sides
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // 2. Helper to evaluate if the adjacent block can accept our MatterCapability
// 2. Helper to evaluate if the adjacent block can accept our MatterCapability
    private boolean canConnectTo(LevelAccessor level, BlockPos currentPos, Direction direction) {
        // Capabilities are only exposed on full Level instances (ServerLevel / ClientLevel)
        if (level instanceof Level fullLevel) {
            BlockPos neighborPos = currentPos.relative(direction);
            // Check if the neighbor block exposes our custom matter capability on that face
            return fullLevel.getCapability(MatterCapabilities.MATTER_HANDLER, neighborPos, direction.getOpposite()) != null;
        }
        return false;
    }

    // 3. Set the state correctly when the user places the block
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return this.defaultBlockState()
                .setValue(NORTH, canConnectTo(level, pos, Direction.NORTH))
                .setValue(SOUTH, canConnectTo(level, pos, Direction.SOUTH))
                .setValue(EAST, canConnectTo(level, pos, Direction.EAST))
                .setValue(WEST, canConnectTo(level, pos, Direction.WEST))
                .setValue(UP, canConnectTo(level, pos, Direction.UP))
                .setValue(DOWN, canConnectTo(level, pos, Direction.DOWN));
    }

    // 4. Update connections dynamically when neighboring blocks change
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty property = switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
        return state.setValue(property, canConnectTo(level, pos, direction));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UniversalMatterDuctBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                UniversalMatterDuctBlockEntity.TYPE.get(),
                UniversalMatterDuctBlockEntity::serverTick);
    }
}