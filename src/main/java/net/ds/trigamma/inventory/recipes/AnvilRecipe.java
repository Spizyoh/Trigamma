package net.ds.trigamma.inventory.recipes;

import net.ds.trigamma.TriGamma;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record AnvilRecipe(
        String id,
        String translationKey,
        AnvilTier requiredTier,
        List<ItemStack> outputs,
        List<IngredientCost> ingredients
) {
    public AnvilRecipe(String id, AnvilTier requiredTier, List<ItemStack> outputs, List<IngredientCost> ingredients) {
        this(id, "category." + TriGamma.MODID + ".anvil_recipe." + id, requiredTier, outputs, ingredients);
    }

    public AnvilRecipe(String id, AnvilTier requiredTier, ItemStack output, List<IngredientCost> ingredients) {
        this(id, requiredTier, List.of(output), ingredients);
    }

    public ItemStack previewOutput() {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst();
    }

    public record IngredientCost(ItemStack item, int count) {}
}