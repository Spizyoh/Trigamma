package net.ds.trigamma.inventory.fluid;

import net.minecraft.resources.ResourceLocation;
import java.util.Set;

public interface IMatter {
    ResourceLocation id();
    MatterPhase phase();
    Set<PropertyTag> tags();
    int color(); // For pipe rendering overlay

    default boolean hasTag(PropertyTag tag) {
        return tags().contains(tag);
    }
}