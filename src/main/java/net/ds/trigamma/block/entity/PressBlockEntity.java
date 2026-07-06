package net.ds.trigamma.block.entity;


import net.ds.trigamma.block.ModBlockEntities;
import net.ds.trigamma.inventory.recipes.ModRecipes;
import net.ds.trigamma.inventory.recipes.PressingRecipe;
import net.ds.trigamma.inventory.recipes.PressingRecipeInput;
import net.ds.trigamma.item.StampItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class PressBlockEntity extends BlockEntity {

    /** How many crank turns it takes to complete one press cycle. */
    private static final int TURNS_TO_PRESS = 4;
    /** Exhaustion added per crank turn - same scale as sprinting/mining costs. */
    private static final float EXHAUSTION_PER_TURN = 3.0F;
    /** Minimum food level required to operate the press (like sprinting requires food > 6). */
    private static final int MIN_FOOD_LEVEL = 1;

    private ItemStack ingotStack = ItemStack.EMPTY;
    private ItemStack stampStack = ItemStack.EMPTY;
    private ItemStack outputStack = ItemStack.EMPTY;

    /** 0 = hammer fully up, TURNS_TO_PRESS = fully down / mid-craft. Drives the visual STAGE property. */
    private int hammerStage = 0;

    public PressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESS.get(), pos, state);
    }

    // ---------------------------------------------------------------
    // Public interaction API - called by PressBlock from useItemOn / useWithoutItem
    // ---------------------------------------------------------------

    /** Called when the player right-clicks the crank (upper half) with any/no item. */
    public void crank(Player player) {
        if (level == null || level.isClientSide) return;

        if (!outputStack.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.trigamma.metalpress.press_full"), true);
            return;
        }

        if (ingotStack.isEmpty() || stampStack.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.trigamma.metalpress.needs_ingot_and_stamp"), true);
            return;
        }

        Optional<PressingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(ModRecipes.PRESSING_TYPE.get(),
                        new PressingRecipeInput(ingotStack, stampStack), level)
                .map(holder -> holder.value());

        if (recipe.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.trigamma.metalpress.no_recipe"), true);
            return;
        }

        if (player.getFoodData().getFoodLevel() <= MIN_FOOD_LEVEL && !player.isCreative()) {
            player.displayClientMessage(Component.translatable("message.trigamma.metalpress.too_hungry"), true);
            return;
        }

        // --- consume hunger/saturation for this turn ---
        if (!player.isCreative()) {
            player.causeFoodExhaustion(EXHAUSTION_PER_TURN);
        }

        level.playSound(null, worldPosition, SoundEvents.ANVIL_STEP, SoundSource.BLOCKS, 0.6F, 0.8F);

        hammerStage++;
        setChanged();
        syncToClient();

        if (hammerStage >= TURNS_TO_PRESS) {
            completePress(recipe.get(), player);
        }
    }

    private void completePress(PressingRecipe recipe, Player player) {
        if (level == null) return;

        outputStack = recipe.assemble(new PressingRecipeInput(ingotStack, stampStack), level.registryAccess());

        ingotStack.shrink(1);
        if (ingotStack.isEmpty()) {
            ingotStack = ItemStack.EMPTY;
        }

        // wear down the stamp
        if (stampStack.getItem() instanceof StampItem && stampStack.isDamageableItem()) {
            stampStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            if (stampStack.isEmpty()) {
                stampStack = ItemStack.EMPTY;
            }
        }

        level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 0.7F);

        hammerStage = 0;
        setChanged();
        syncToClient();
    }

    /** Right-clicked the lower half with an item in hand. Returns true if the item was consumed/handled. */
    public boolean insertItem(ItemStack heldStack, Player player) {
        if (level == null || level.isClientSide) return true;

        if (heldStack.isEmpty()) {
            return false;
        }

        if (heldStack.getItem() instanceof StampItem) {
            return insertStamp(heldStack, player);
        }

        return insertIngot(heldStack, player);
    }

    private boolean insertStamp(ItemStack heldStack, Player player) {
        if (!stampStack.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.trigamma.metalpress.stamp_installed"), true);
            return false;
        }
        stampStack = heldStack.split(1);
        setChanged();
        syncToClient();
        return true;
    }

    private boolean insertIngot(ItemStack heldStack, Player player) {
        if (!ingotStack.isEmpty()) {
            player.displayClientMessage(Component.literal(
                    "Ingot slot has: " + ingotStack.getHoverName().getString()
                            + ", held: " + heldStack.getHoverName().getString()
            ), false);

            if (!ItemStack.isSameItemSameComponents(ingotStack, heldStack)) {
                player.displayClientMessage(Component.translatable("message.trigamma.metalpress.different_ingot"), true);
                return false;
            }
            int space = ingotStack.getMaxStackSize() - ingotStack.getCount();
            if (space <= 0) {
                player.displayClientMessage(Component.translatable("message.trigamma.metalpress.ingot_full"), true);
                return false;
            }
            int moved = Math.min(space, heldStack.getCount());
            ingotStack.grow(moved);
            heldStack.shrink(moved);
        } else {
            int moved = Math.min(heldStack.getCount(), heldStack.getMaxStackSize());
            ingotStack = heldStack.copyWithCount(moved);
            heldStack.shrink(moved);
        }
        setChanged();
        syncToClient();
        return true;
    }

    /**
     * Right-clicked the lower half with an empty hand.
     * Takes the output if present, otherwise (if sneaking) returns the stamp.
     */
    public void tryExtract(Player player, boolean sneaking) {
        if (level == null || level.isClientSide) return;

        if (!outputStack.isEmpty()) {
            giveOrDrop(player, outputStack);
            outputStack = ItemStack.EMPTY;
        } else if (!stampStack.isEmpty()) {
            giveOrDrop(player, stampStack);
            stampStack = ItemStack.EMPTY;
        } else if (!ingotStack.isEmpty()) {
            giveOrDrop(player, ingotStack);
            ingotStack = ItemStack.EMPTY;
        } else {
            return;
        }

        setChanged();
        syncToClient();
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;

        dropStack(ingotStack);
        dropStack(stampStack);
        dropStack(outputStack);

        ingotStack = ItemStack.EMPTY;
        stampStack = ItemStack.EMPTY;
        outputStack = ItemStack.EMPTY;

        setChanged();
    }

    private void dropStack(ItemStack stack) {
        if (!stack.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(
                    level,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    stack.copy()
            );
        }
    }

    public int getHammerStage() {
        return hammerStage;
    }

    /**
     * Pushes hammerStage into the STAGE blockstate property on both halves so the model/texture
     * updates client-side, and also syncs the BE data (for the ingot/stamp/output stacks).
     */
    private void syncToClient() {
        if (level == null || level.isClientSide) return;

        BlockState lowerState = level.getBlockState(worldPosition);
        if (lowerState.getBlock() instanceof net.ds.trigamma.block.PressBlock
                && lowerState.getValue(net.ds.trigamma.block.PressBlock.STAGE) != hammerStage) {
            level.setBlock(worldPosition, lowerState.setValue(net.ds.trigamma.block.PressBlock.STAGE, hammerStage), Block.UPDATE_CLIENTS);
        }

        BlockPos upperPos = worldPosition.above();
        BlockState upperState = level.getBlockState(upperPos);
        if (upperState.getBlock() instanceof net.ds.trigamma.block.PressBlock
                && upperState.getValue(net.ds.trigamma.block.PressBlock.STAGE) != hammerStage) {
            level.setBlock(upperPos, upperState.setValue(net.ds.trigamma.block.PressBlock.STAGE, hammerStage), Block.UPDATE_CLIENTS);
        }

        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    // ---------------------------------------------------------------
    // Saving / loading / syncing
    // ---------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Ingot", ingotStack.saveOptional(registries));
        tag.put("Stamp", stampStack.saveOptional(registries));
        tag.put("Output", outputStack.saveOptional(registries));
        tag.putInt("HammerStage", hammerStage);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ingotStack = ItemStack.parseOptional(registries, tag.getCompound("Ingot"));
        stampStack = ItemStack.parseOptional(registries, tag.getCompound("Stamp"));
        outputStack = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        hammerStage = tag.getInt("HammerStage");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
