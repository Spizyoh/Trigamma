package net.ds.trigamma.inventory.fluid;

import java.util.Optional;

public class PipeMatterTank implements IMatterHandler {
    private IMatter currentMatter = null;
    private int amount = 0;
    private final int capacity;

    public PipeMatterTank(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Tries to fill the pipe. Enforces phase matching.
     */
    @Override
    public int fill(IMatter resource, int fillAmount, boolean simulate) {
        if (resource == null || fillAmount <= 0) return 0;

        // Rule: Cannot mix fluids and gases, or different types of the same phase
        if (currentMatter != null && !currentMatter.id().equals(resource.id())) {
            return 0;
        }

        int maxFill = Math.min(capacity - amount, fillAmount);
        if (maxFill <= 0) return 0;

        if (!simulate) {
            if (currentMatter == null) {
                this.currentMatter = resource; // Lock the pipe to this matter type
            }
            this.amount += maxFill;
        }

        return maxFill;
    }

    /**
     * Drains matter from the pipe.
     */
    @Override
    public int drain(int drainAmount, boolean simulate) {
        if (currentMatter == null || amount <= 0 || drainAmount <= 0) return 0;

        int maxDrain = Math.min(amount, drainAmount);
        if (!simulate) {
            this.amount -= maxDrain;
            if (this.amount <= 0) {
                this.currentMatter = null; // Unlock the pipe phase entirely
            }
        }
        return maxDrain;
    }

    // --- Getters ---

    @Override
    public MatterPhase getPhase() {
        return currentMatter == null ? MatterPhase.EMPTY : currentMatter.phase();
    }

    @Override
    public Optional<IMatter> getCurrentMatter() {
        return Optional.ofNullable(currentMatter);
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean isEmpty() {
        return currentMatter == null || amount <= 0;
    }
}