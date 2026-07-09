package net.ds.trigamma.inventory.fluid;

import java.util.Optional;

public interface IMatterBuffer {
    Optional<IMatter> getMatter();
    int getAmount();
    int getCapacity();

    /** Attempts to add matter; returns the amount actually accepted. */
    int fill(IMatter matter, int amount);

    /** Attempts to remove up to `amount`; returns the amount actually removed. */
    int drain(int amount);

    default boolean isEmpty() {
        return getAmount() <= 0;
    }
}