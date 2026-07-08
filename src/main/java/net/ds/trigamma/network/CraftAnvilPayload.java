package net.ds.trigamma.network;

import net.ds.trigamma.TriGamma;
import net.ds.trigamma.inventory.gui.CustomAnvilMenu;
import net.ds.trigamma.inventory.recipes.AnvilRecipe;
import net.ds.trigamma.inventory.recipes.ModAnvilRecipes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record CraftAnvilPayload(String recipeId) implements CustomPacketPayload {

    // Create a unique Network ID tag for this packet
    public static final Type<CraftAnvilPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TriGamma.MODID, "craft_anvil_packet"));

    // Codec that writes/reads the recipe string to/from network bytes
    public static final StreamCodec<FriendlyByteBuf, CraftAnvilPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.recipeId),
            buf -> new CraftAnvilPayload(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // This handles the server-side action when the packet arrives
    public static void handleServer(final CraftAnvilPayload payload, final IPayloadContext context) {
        // Enqueue the work back onto the main server thread to prevent concurrent modification glitches
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // Find matching recipe inside your data list
                AnvilRecipe matchedRecipe = ModAnvilRecipes.RECIPES.stream()
                        .filter(r -> r.id().equals(payload.recipeId()))
                        .findFirst()
                        .orElse(null);

                if (matchedRecipe != null && hasAllIngredients(player, matchedRecipe)) {
                    if (!(player.containerMenu instanceof CustomAnvilMenu anvilMenu)) {
                        return;
                    }

                    if (!anvilMenu.getAnvilTier().canCraft(matchedRecipe.requiredTier())) {
                        return;
                    }


                    // 1. Consume the materials out of the user's pockets
                    for (AnvilRecipe.IngredientCost ingredient : matchedRecipe.ingredients()) {
                        shrinkPlayerItem(player, ingredient.item(), ingredient.count());
                    }

                    // 2. Add the freshly crafted product to the user's inventory
                    for (ItemStack output : matchedRecipe.outputs()) {
                        ItemStack outputClone = output.copy();

                        if (!player.getInventory().add(outputClone) && !outputClone.isEmpty()) {
                            player.drop(outputClone, false);
                        }
                    }
                }
            }
        });
    }

    private static boolean hasAllIngredients(ServerPlayer player, AnvilRecipe recipe) {
        for (AnvilRecipe.IngredientCost ingredient : recipe.ingredients()) {
            int currentCount = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, ingredient.item())) {
                    currentCount += stack.getCount();
                }
            }
            if (currentCount < ingredient.count()) return false;
        }
        return true;
    }

    private static void shrinkPlayerItem(ServerPlayer player, ItemStack itemToConsume, int amountNeeded) {
        int leftToConsume = amountNeeded;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && ItemStack.isSameItem(stack, itemToConsume)) {
                int stackAmount = stack.getCount();
                if (stackAmount >= leftToConsume) {
                    stack.shrink(leftToConsume);
                    return;
                } else {
                    leftToConsume -= stackAmount;
                    stack.setCount(0);
                }
            }
        }
    }
}
