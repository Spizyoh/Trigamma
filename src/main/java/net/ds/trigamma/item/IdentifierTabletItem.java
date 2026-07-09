package net.ds.trigamma.item;

import net.ds.trigamma.block.entity.BoilerBlockEntity;
import net.ds.trigamma.block.entity.UniversalMatterDuctBlockEntity;
import net.ds.trigamma.client.IdentifierTabletClientHandler;
import net.ds.trigamma.inventory.fluid.IMatter;
import net.ds.trigamma.inventory.fluid.MatterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class IdentifierTabletItem extends Item {
    public IdentifierTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null || level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);

        Optional<BoilerBlockEntity> boilerOpt = BoilerBlockEntity.resolve(level, pos);
        if (boilerOpt.isPresent()) {
            BoilerBlockEntity boiler = boilerOpt.get();
            Optional<IMatter> selectedMatter = getSelectedMatter(stack);
            if (selectedMatter.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.trigamma.tablet_empty"));
                return InteractionResult.SUCCESS;
            }

            IMatter matter = selectedMatter.get();
            boolean success = boiler.setRecipeFromInput(matter);
            Component matterName = Component.translatable(matter.translationKey());

            if (success) {
                player.sendSystemMessage(Component.translatable("message.trigamma.boiler_recipe_set", matterName));
            } else {
                player.sendSystemMessage(Component.translatable("message.trigamma.boiler_no_recipe", matterName));
            }
            return InteractionResult.SUCCESS;
        }

        if (be instanceof UniversalMatterDuctBlockEntity duct) {
            Optional<IMatter> selectedMatter = getSelectedMatter(stack);

            if (selectedMatter.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.trigamma.tablet_empty"));
                return InteractionResult.SUCCESS;
            }

            IMatter matter = selectedMatter.get();
            Component matterName = Component.translatable(matter.translationKey());

            if (player.isShiftKeyDown()) {
                int updatedCount = configureNetwork(level, pos, matter);
                player.sendSystemMessage(Component.translatable("message.trigamma.duct_configured",
                        updatedCount, matterName));
            } else {
                duct.setFilterMatter(matter);
                player.sendSystemMessage(Component.translatable("message.trigamma.duct_locked", matterName));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            IdentifierTabletClientHandler.openScreen(hand);
        }
        return InteractionResultHolder.success(stack);
    }

    private int configureNetwork(Level level, BlockPos startPos, IMatter matter) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        int configuredCount = 0;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            BlockEntity be = level.getBlockEntity(current);
            if (be instanceof UniversalMatterDuctBlockEntity duct) {
                // New code
                duct.setFilterMatter(matter);
                configuredCount++;

                for (Direction dir : Direction.values()) {
                    BlockPos next = current.relative(dir);
                    if (!visited.contains(next)) {
                        queue.add(next);
                    }
                }
            }
        }
        return configuredCount;
    }

    public Optional<IMatter> getSelectedMatter(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return Optional.empty();

        CompoundTag tag = customData.copyTag();
        if (!tag.contains("SelectedMatter")) return Optional.empty();

        return MatterRegistry.get(ResourceLocation.parse(tag.getString("SelectedMatter")));
    }
}