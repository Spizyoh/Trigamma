package net.ds.trigamma.inventory.fluid;

import java.util.Set;

public enum PropertyTag {
    COMBUSTIBLE,
    CORROSIVE,
    COOLANT,
    ANTIMATTER,
    FLAMMABLE,
    POLLUTING,
    RADIOACTIVE,
    STRONGLY_CORROSIVE,
    TOXIC,
    VISCOUS; // Fluid exclusive

    /**
     * Validates if a set of tags is allowed for a specific phase.
     */
    public static void validateTags(Set<PropertyTag> tags, MatterPhase phase) {
        if (phase == MatterPhase.GAS && tags.contains(VISCOUS)) {
            throw new IllegalArgumentException("Gases cannot have the VISCOUS property tag!");
        }
    }
}