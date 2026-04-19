package net.ds.trigamma.item;

import net.ds.trigamma.TriGamma;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TriGamma.MODID);

    public static final Supplier<CreativeModeTab> TRIGAMMA_ITEMS_TAB = CREATIVE_MODE_TAB.register("trigamma_items_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.COAL_POWDER.get()))
                    .title(Component.translatable("creativetab.trigamma.trigamma_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.COAL_POWDER);
                    })
                    .build());
    public static final Supplier<CreativeModeTab> TRIGAMMA_BLOCKS_TAB = CREATIVE_MODE_TAB.register("trigamma_blocks_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(TriGamma.MODID, "trigamma_items_tab"))
                    .icon(() -> new ItemStack(ModItems.COAL_POWDER.get()))
                    .title(Component.translatable("creativetab.trigamma.trigamma_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.COAL_POWDER);
                    })
                    .build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }

}
