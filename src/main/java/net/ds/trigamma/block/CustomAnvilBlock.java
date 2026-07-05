package net.ds.trigamma.block;
import net.ds.trigamma.inventory.gui.CustomAnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CustomAnvilBlock extends FallingBlock {
    // 1. Define the codec field using simpleCodec
    public static final MapCodec<CustomAnvilBlock> CODEC = simpleCodec(CustomAnvilBlock::new);

    private static final VoxelShape SHAPE = FallingBlock.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public CustomAnvilBlock(Properties properties) {
        super(properties);
    }

    // 2. Override the abstract codec() method requested by FallingBlock
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

            // 1. Create a regular vanilla SimpleMenuProvider
            MenuProvider menuProvider = new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new CustomAnvilMenu(containerId, playerInventory),
                    Component.literal("Tier 1 Anvil")
            );

            // 2. Open the menu using NeoForge's custom openMenu extension.
            // The second parameter (buffer -> {}) feeds the exact RegistryFriendlyByteBuf the client needs.
            serverPlayer.openMenu(menuProvider, buffer -> {
                // Leave this empty!
                // This satisfies the IMenuTypeExtension buffer requirement without needing extra code.
            });
        }
        return InteractionResult.SUCCESS;
    }
}


