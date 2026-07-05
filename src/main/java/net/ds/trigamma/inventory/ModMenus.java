package net.ds.trigamma.inventory;

import net.ds.trigamma.TriGamma; // Replace with your main mod class import
import net.ds.trigamma.inventory.gui.CustomAnvilMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, TriGamma.MODID);

    // 2. Change Supplier to DeferredHolder to prevent early evaluation crashes
    public static final DeferredHolder<MenuType<?>, MenuType<CustomAnvilMenu>> CUSTOM_ANVIL_MENU =
            MENUS.register("custom_anvil_menu", () -> IMenuTypeExtension.create(CustomAnvilMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
