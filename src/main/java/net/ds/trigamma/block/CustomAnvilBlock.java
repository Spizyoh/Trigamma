package net.ds.trigamma.block;

import com.mojang.serialization.MapCodec;
import net.ds.trigamma.inventory.gui.CustomAnvilMenu;
import net.ds.trigamma.inventory.recipes.AnvilTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CustomAnvilBlock extends FallingBlock {
    public static final MapCodec<CustomAnvilBlock> CODEC = simpleCodec(properties -> new CustomAnvilBlock(properties, AnvilTier.T1));

    private static final VoxelShape SHAPE = FallingBlock.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    private final AnvilTier tier;

    public CustomAnvilBlock(Properties properties, AnvilTier tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider menuProvider = new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) ->
                            new CustomAnvilMenu(containerId, playerInventory, this.tier),
                    Component.translatable("container.trigamma.anvil." + this.tier.getSerializedName())
            );

            serverPlayer.openMenu(menuProvider, buffer -> {
                buffer.writeEnum(this.tier);
            });
        }

        return InteractionResult.SUCCESS;
    }
}


