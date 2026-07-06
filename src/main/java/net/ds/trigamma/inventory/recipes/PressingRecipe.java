package net.ds.trigamma.inventory.recipes;

import net.ds.trigamma.item.StampType;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class PressingRecipe implements Recipe<PressingRecipeInput> {

    private final Ingredient ingot;
    private final StampType stampType;
    private final ItemStack result;

    public PressingRecipe(Ingredient ingot, StampType stampType, ItemStack result) {
        this.ingot = ingot;
        this.stampType = stampType;
        this.result = result;
    }

    public Ingredient getIngot() {
        return ingot;
    }

    public StampType getStampType() {
        return stampType;
    }

    @Override
    public boolean matches(PressingRecipeInput input, Level level) {
        return ingot.test(input.ingot()) && stampType.matches(input.stamp());
    }

    @Override
    public ItemStack assemble(PressingRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<? extends Recipe<PressingRecipeInput>> getSerializer() {
        return ModRecipes.PRESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<PressingRecipeInput>> getType() {
        return ModRecipes.PRESSING_TYPE.get();
    }
}
