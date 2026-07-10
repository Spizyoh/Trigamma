package net.ds.trigamma.inventory.fluid;

import java.util.Optional;

/**
 * Generic contract for anything that can send/receive Matter (fluids/gases) across a
 * block face - pipe segments, machine ports, tanks, etc.
 * <p>
 * This is the type now exposed by {@link MatterCapabilities#MATTER_HANDLER}, replacing the
 * old hard dependency on the concrete {@link PipeMatterTank} class so that other objects
 * (e.g. MachinePortBlockEntity) can be plugged into the same capability without wrapping
 * themselves in a fake pipe.
 */
public interface IMatterHandler {

    /**
     * Attempts to push matter into this handler.
     * @return the amount actually accepted.
     */
    int fill(IMatter resource, int amount, boolean simulate);

    /**
     * Attempts to pull matter out of this handler.
     * @return the amount actually removed.
     */
    int drain(int amount, boolean simulate);

    MatterPhase getPhase();

    Optional<IMatter> getCurrentMatter();

    int getAmount();

    int getCapacity();

    default boolean isEmpty() {
        return getAmount() <= 0;
    }
}