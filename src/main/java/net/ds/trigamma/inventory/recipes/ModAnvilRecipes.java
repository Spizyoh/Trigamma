package net.ds.trigamma.inventory.recipes;

import net.ds.trigamma.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class ModAnvilRecipes {
    public static final List<AnvilRecipe> RECIPES = new ArrayList<>();

    static {
        // Example Recipe 1: Crafting an Iron Block
        RECIPES.add(new AnvilRecipe(
                "mixed_iron_block",
                AnvilTier.T2,
                List.of(new ItemStack(Items.IRON_BLOCK, 1)),
                List.of(
                        new AnvilRecipe.IngredientCost(new ItemStack(Items.IRON_INGOT), 6),
                        new AnvilRecipe.IngredientCost(new ItemStack(Items.COPPER_INGOT), 3)
                )
        ));

        RECIPES.add(new AnvilRecipe(
                "forged_copper_coil",
                AnvilTier.T1,
                List.of(new ItemStack(ModItems.COPPER_COIL.get(), 1)),
                List.of(
                        new AnvilRecipe.IngredientCost(new ItemStack(Items.IRON_INGOT), 1),
                        new AnvilRecipe.IngredientCost(new ItemStack(ModItems.COPPER_WIRE.get()), 2)
                )
        ));

        // Add your custom items here, for example:
        // RECIPES.add(new AnvilRecipe("titanium_plate", new ItemStack(ModItems.TITANIUM_PLATE.get()), ...));
    }
}
