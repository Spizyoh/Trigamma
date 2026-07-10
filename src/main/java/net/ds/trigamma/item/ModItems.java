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
    public static final DeferredItem<Item> COPPER_WIRE = ITEMS.register("copper_wire",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_GEAR = ITEMS.register("gold_gear",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_BOLT = ITEMS.register("iron_bolt",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.register("steel_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_INGOT = ITEMS.register("aluminium_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BERYLLIUM_INGOT = ITEMS.register("beryllium_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MAGNESIUM_INGOT = ITEMS.register("magnesium_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TUNGSTEN_INGOT = ITEMS.register("tungsten_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COBALT_INGOT = ITEMS.register("cobalt_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHROMIUM_INGOT = ITEMS.register("chromium_ingot",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> IRON_PLATE = ITEMS.register("iron_plate",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_PLATE = ITEMS.register("steel_plate",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_PLATE = ITEMS.register("aluminium_plate",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BERYLLIUM_PLATE = ITEMS.register("beryllium_plate",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MAGNESIUM_PLATE = ITEMS.register("magnesium_plate",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TUNGSTEN_PLATE = ITEMS.register("tungsten_plate",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COBALT_PLATE = ITEMS.register("cobalt_plate",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<StampItem> PLATE_STAMP = ITEMS.register("plate_stamp",
            () -> new StampItem(StampType.PLATE, new Item.Properties().durability(64)));

    public static final DeferredItem<StampItem> WIRE_STAMP = ITEMS.register("wire_stamp",
            () -> new StampItem(StampType.WIRE, new Item.Properties().durability(64)));

    public static final DeferredItem<StampItem> GEAR_STAMP = ITEMS.register("gear_stamp",
            () -> new StampItem(StampType.GEAR, new Item.Properties().durability(64)));

    public static final DeferredItem<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new GeigerCounterItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> IDENTIFIER_TABLET = ITEMS.register("identifier_tablet",
            () -> new IdentifierTabletItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DEBUG_PIPE_INSERTER = ITEMS.register("debug_pipe_inserter",
            () -> new DebugPipeInserterItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
