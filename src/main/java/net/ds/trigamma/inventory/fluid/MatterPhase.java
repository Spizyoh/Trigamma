package net.ds.trigamma.inventory.fluid;

public enum MatterPhase {
    EMPTY,
    FLUID,
    GAS;

    public boolean IsPresent() {
        return this != EMPTY;
    }
}
