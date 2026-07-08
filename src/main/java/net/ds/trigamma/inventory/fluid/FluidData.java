package net.ds.trigamma.inventory.fluid;

import net.minecraft.resources.ResourceLocation;
import java.util.Set;

public record FluidData(
        ResourceLocation id,
        Set<PropertyTag> tags,
        int color,
        int viscosity // Specific to fluids
) implements IMatter {

    public FluidData {
        PropertyTag.validateTags(tags, MatterPhase.FLUID);
    }

    @Override
    public MatterPhase phase() {
        return MatterPhase.FLUID;
    }
}