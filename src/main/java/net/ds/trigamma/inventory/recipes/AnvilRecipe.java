package net.ds.trigamma.inventory.recipes;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public record AnvilRecipe(String id, ItemStack output, List<IngredientCost> ingredients) {

    public record IngredientCost(ItemStack item, int count) {}

    // Check if the recipe name matches the text search bar string
    public boolean matchesSearch(String query) {
        if (query.isEmpty()) return true;
        return output.getHoverName().getString().toLowerCase().contains(query.toLowerCase());
    }
}
