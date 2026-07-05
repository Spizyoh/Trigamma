package net.ds.trigamma.data;

import net.ds.trigamma.block.ModBlocks;
import net.ds.trigamma.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class CraftingRecipeProvider extends RecipeProvider {

    // 1. Correct Constructor for NeoForge 1.21.1
    public CraftingRecipeProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
        super(packOutput, registries); // Matches PackOutput and CompletableFuture
    }

    // 2. Correct buildRecipes method for NeoForge 1.21.1
    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        // --- 1. SHAPED RECIPES (Require a specific pattern grid) ---
        registerShapedRecipes(recipeOutput);

        // --- 2. SHAPELESS RECIPES (Items can be placed anywhere in the grid) ---
        registerShapelessRecipes(recipeOutput);
    }

    /**
     * Put all your shaped (pattern-based) recipes here.
     */
    private void registerShapedRecipes(RecipeOutput output) {
        // Example: Crafting a Diamond Block from 9 Diamonds
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.T1_ANVIL.asItem())
                .define('#', Items.IRON_INGOT)
                .define('*', Items.IRON_BLOCK)
                .define('A', Items.ANVIL)
                .pattern("#*#")
                .pattern(" A ")
                .pattern("#*#")
                .unlockedBy("has_anvil", has(Items.ANVIL))
                .save(output);

        /* Example: Crafting a Sword (using common NeoForge Tags)
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, Items.DIAMOND_SWORD)
                .define('I', Tags.Items.INGOTS_IRON) // Uses any iron ingot
                .define('S', Items.STICK)
                .pattern("I")
                .pattern("I")
                .pattern("S")
                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                .save(output);
        */
    }

    /**
     * Put all your shapeless recipes here.
     */
    private void registerShapelessRecipes(RecipeOutput output) {
        /* Example: Turning a Gold Block back into 9 Gold Ingots
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.GOLD_INGOT, 9)
                .requires(Items.GOLD_BLOCK)
                .unlockedBy("has_gold_block", has(Items.GOLD_BLOCK))
                .save(output);

        Example: Crafting Fire Charge from Gunpowder, Blaze Powder, and Coal
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.FIRE_CHARGE, 3)
                .requires(Items.GUNPOWDER)
                .requires(Items.BLAZE_POWDER)
                .requires(Items.COAL)
                .unlockedBy("has_blaze_powder", has(Items.BLAZE_POWDER))
                .save(output);
         */
    }
}
