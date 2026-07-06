package net.ds.trigamma.inventory.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ds.trigamma.item.StampType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PressingRecipeSerializer implements RecipeSerializer<PressingRecipe> {

    public static final MapCodec<PressingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("ingot").forGetter(PressingRecipe::getIngot),
                    StampType.CODEC.fieldOf("stamp_type").forGetter(PressingRecipe::getStampType),
                    ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.getResultItem(null))
            ).apply(instance, PressingRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PressingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, PressingRecipe::getIngot,
            StampType.STREAM_CODEC, PressingRecipe::getStampType,
            ItemStack.STREAM_CODEC, recipe -> recipe.getResultItem(null),
            PressingRecipe::new
    );

    @Override
    public MapCodec<PressingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, PressingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
