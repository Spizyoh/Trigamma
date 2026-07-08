package net.ds.trigamma.inventory.recipes;

import java.util.Locale;

public enum AnvilTier {
    T1(1),
    T2(2),
    T3(3);

    private final int level;

    AnvilTier(int level) {
        this.level = level;
    }

    public boolean canCraft(AnvilTier requiredTier) {
        return this.level >= requiredTier.level;
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}