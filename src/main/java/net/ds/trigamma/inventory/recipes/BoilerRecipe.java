package net.ds.trigamma.inventory.recipes;

import net.ds.trigamma.inventory.fluid.IMatter;

public record BoilerRecipe(
        IMatter input,
        int inputAmount,
        IMatter output,
        int outputAmount,
        int baseProcessTicks // TODO: gate progress on TU once thermal units exist
) {}