package net.ds.trigamma.inventory.fluid;

import net.minecraft.resources.ResourceLocation;
import java.util.Set;

public interface IMatter {
    ResourceLocation id();
    MatterPhase phase();
    Set<PropertyTag> tags();
    int color();

    default boolean hasTag(PropertyTag tag) {
        return tags().contains(tag);
    }

    default String translationKey() {
        String category = switch (phase()) {
            case FLUID -> "fluid";
            case GAS -> "gas";
            case EMPTY -> "matter";
        };
        return category + "." + id().getNamespace() + "." + id().getPath();
    }
}