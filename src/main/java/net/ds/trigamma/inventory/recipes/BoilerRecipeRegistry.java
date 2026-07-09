package net.ds.trigamma.inventory.recipes;

import net.ds.trigamma.inventory.fluid.MatterRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BoilerRecipeRegistry {
    // Keyed by the INPUT matter's id — only one recipe per input is allowed.
    private static final Map<ResourceLocation, BoilerRecipe> BY_INPUT = new HashMap<>();

    // --- Example recipes, replace/expand with real ones ---
    public static final BoilerRecipe WATER_TO_STEAM = register(new BoilerRecipe(
            MatterRegistry.WATER, 1,
            MatterRegistry.STEAM, 100,
            200
    ));

    private static BoilerRecipe register(BoilerRecipe recipe) {
        ResourceLocation inputId = recipe.input().id();
        if (BY_INPUT.containsKey(inputId)) {
            throw new IllegalStateException(
                    "Duplicate boiler recipe for input matter: " + inputId +
                            " — only one recipe per input matter is allowed."
            );
        }
        BY_INPUT.put(inputId, recipe);
        return recipe;
    }

    public static Optional<BoilerRecipe> getByInput(ResourceLocation inputMatterId) {
        return Optional.ofNullable(BY_INPUT.get(inputMatterId));
    }
}
