package net.ds.trigamma.inventory.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class MatterBuffer implements IMatterBuffer {
    private final int capacity;
    private IMatter matter;
    private int amount;

    public MatterBuffer(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public Optional<IMatter> getMatter() {
        return Optional.ofNullable(matter);
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
    public int fill(IMatter incoming, int requested) {
        if (requested <= 0) return 0;

        // Buffer is empty -> accept the new matter type
        if (this.matter == null) {
            this.matter = incoming;
            int accepted = Math.min(requested, capacity);
            this.amount = accepted;
            return accepted;
        }

        // Buffer holds a different matter -> reject entirely (no mixing)
        if (!this.matter.id().equals(incoming.id())) {
            return 0;
        }

        int space = capacity - this.amount;
        int accepted = Math.min(requested, space);
        this.amount += accepted;
        return accepted;
    }

    @Override
    public int drain(int requested) {
        if (matter == null || requested <= 0) return 0;

        int removed = Math.min(requested, amount);
        amount -= removed;
        if (amount <= 0) {
            amount = 0;
            matter = null;
        }
        return removed;
    }

    public void setDirectly(IMatter matter, int amount) {
        this.matter = matter;
        this.amount = Math.min(amount, capacity);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Amount", amount);
        if (matter != null) {
            tag.putString("Matter", matter.id().toString());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        this.amount = tag.getInt("Amount");
        if (tag.contains("Matter")) {
            ResourceLocation id = ResourceLocation.parse(tag.getString("Matter"));
            this.matter = MatterRegistry.get(id).orElse(null);
            if (this.matter == null) this.amount = 0; // matter no longer registered
        } else {
            this.matter = null;
        }
    }
}