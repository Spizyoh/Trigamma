package net.ds.trigamma.inventory.recipes;

import net.ds.trigamma.TriGamma;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, TriGamma.MODID); // <-- your modid

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, TriGamma.MODID); // <-- your modid

    public static final DeferredHolder<RecipeSerializer<?>, PressingRecipeSerializer> PRESSING_SERIALIZER =
            SERIALIZERS.register("pressing", PressingRecipeSerializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<PressingRecipe>> PRESSING_TYPE =
            TYPES.register("pressing", () -> new RecipeType<PressingRecipe>() {
                @Override
                public String toString() {
                    return "trigamma:pressing";
                }
            });

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
        TYPES.register(modBus);
    }
}
