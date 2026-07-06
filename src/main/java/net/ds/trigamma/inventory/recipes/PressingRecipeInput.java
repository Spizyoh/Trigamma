package net.ds.trigamma.inventory.recipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * The "inventory view" the press's recipe manager checks against: slot 0 is the ingot,
 * slot 1 is the currently installed stamp.
 */
public record PressingRecipeInput(ItemStack ingot, ItemStack stamp) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> ingot;
            case 1 -> stamp;
            default -> throw new IllegalArgumentException("No such slot: " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
