package net.ds.trigamma.radiation;

import net.ds.trigamma.item.RadioactiveItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for radioactive items.
 *
 * Use this to register radiation values for:
 *  - Vanilla Minecraft items
 *  - Items from other mods you can't subclass
 *  - Your own items, as an alternative to implementing {@link RadioactiveItem}
 *
 * Registration should happen during mod initialisation (e.g. in your mod's constructor
 * or a dedicated setup event), before the first server tick.
 *
 * Items that implement {@link RadioactiveItem} do NOT need to be registered here —
 * their interface method is used automatically. If both exist, the interface takes priority.
 *
 * Example:
 * <pre>{@code
 *   // In your mod init:
 *   RadioactiveItemRegistry.register(Items.ANCIENT_DEBRIS, 8f);
 *   RadioactiveItemRegistry.register(Items.NETHER_GOLD_ORE, 3f);
 *   RadioactiveItemRegistry.register(ModItems.URANIUM_ORE.get(), 80f);
 *   RadioactiveItemRegistry.register(ModItems.PLUTONIUM_INGOT.get(), 200f);
 * }</pre>
 */
public final class RadioactiveItemRegistry {

    private RadioactiveItemRegistry() {}

    /** item → rads per second per single item */
    private static final Map<Item, Float> REGISTRY = new HashMap<>();

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers an item as radioactive.
     *
     * @param item   the item type
     * @param radsPerSecond  rads emitted per second, per single item in the stack
     */
    public static void register(Item item, float radsPerSecond) {
        if (radsPerSecond <= 0f) {
            throw new IllegalArgumentException("Radiation value must be positive for item: " + item);
        }
        REGISTRY.put(item, radsPerSecond);
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    /**
     * Returns the base rads/s for one unit of the item type, or 0 if not radioactive.
     * Checks {@link RadioactiveItem} interface first, then the registry.
     */
    public static float getBaseRadiation(Item item) {
        if (item instanceof RadioactiveItem ri) {
            return ri.getRadiationPerSecond();
        }
        return REGISTRY.getOrDefault(item, 0f);
    }

    /**
     * Returns the total rads/s emitted by the entire stack (base × count).
     */
    public static float getStackRadiation(ItemStack stack) {
        if (stack.isEmpty()) return 0f;
        float base = getBaseRadiation(stack.getItem());
        return base * stack.getCount();
    }

    /**
     * Returns true if the item (or its type in the registry) is radioactive.
     */
    public static boolean isRadioactive(Item item) {
        return item instanceof RadioactiveItem || REGISTRY.containsKey(item);
    }

    public static boolean isRadioactive(ItemStack stack) {
        return !stack.isEmpty() && isRadioactive(stack.getItem());
    }
}