package net.ds.trigamma.inventory.fluid;

import net.minecraft.resources.ResourceLocation;
import java.util.Set;

public record GasData(
        ResourceLocation id,
        Set<PropertyTag> tags,
        int color,
        float dissipationRate // Specific to gases
) implements IMatter {

    public GasData {
        PropertyTag.validateTags(tags, MatterPhase.GAS);
    }

    @Override
    public MatterPhase phase() {
        return MatterPhase.GAS;
    }
}