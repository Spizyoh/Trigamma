package net.ds.trigamma.block;

import net.minecraft.util.StringRepresentable;

/**
 * Which half of the 2-tall Metalworking Press this blockstate represents.
 * LOWER holds the BlockEntity (ingot/stamp/output). UPPER is just the crank visually,
 * but forwards all interactions down to the LOWER half's BlockEntity.
 */
public enum PressPart implements StringRepresentable {
    LOWER("lower"),
    UPPER("upper");

    private final String name;

    PressPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean isUpper() {
        return this == UPPER;
    }
}
