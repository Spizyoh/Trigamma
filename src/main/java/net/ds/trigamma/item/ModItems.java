package net.ds.trigamma.item;

import net.ds.trigamma.TriGamma;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TriGamma.MODID);

    public static final DeferredItem<Item> RAW_LEAD = ITEMS.register("raw_lead",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COAL_POWDER = ITEMS.register("coal_powder",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_COIL = ITEMS.register("copper_coil",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new GeigerCounterItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
