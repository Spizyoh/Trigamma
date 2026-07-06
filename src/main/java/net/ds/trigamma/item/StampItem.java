package net.ds.trigamma.item;

import net.minecraft.world.item.Item;

/**
 * A physical stamp (plate stamp, wire stamp, gear stamp...) that gets inserted
 * into the press's stamp slot. It has durability and wears down 1 point per press cycle.
 */
public class StampItem extends Item {

    private final StampType stampType;

    public StampItem(StampType stampType, Properties properties) {
        // durability example: give it max damage via properties.durability(x) when registering
        super(properties);
        this.stampType = stampType;
    }

    public StampType getStampType() {
        return stampType;
    }
}
