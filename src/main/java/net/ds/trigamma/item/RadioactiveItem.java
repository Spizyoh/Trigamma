package net.ds.trigamma.item;

import net.ds.trigamma.radiation.RadioactiveItemRegistry;
import net.minecraft.world.item.Item;

/**
 * Implement this interface on any Item subclass to mark it as radioactive.
 *
 * The radiation value represents rads per second emitted by a single item in a stack.
 * Stack radiation is additive: a stack of 32 uranium ingots emits 32× the base value.
 *
 * Example usage on a custom item:
 * <pre>{@code
 *   public class UraniumIngotItem extends Item implements RadioactiveItem {
 *       @Override public float getRadiationPerSecond() { return 5f; }
 *   }
 * }</pre>
 *
 * For vanilla or third-party items you cannot subclass, use
 * {@link RadioactiveItemRegistry#register(Item, float)}  instead.
 */
public interface RadioactiveItem {

    /**
     * Returns the radiation this item emits per second, per single item in a stack.
     *
     * @return rads/s per unit (must be &gt; 0)
     */
    float getRadiationPerSecond();
}