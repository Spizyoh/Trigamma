package net.ds.trigamma.client;

import net.ds.trigamma.TriGamma;
import net.ds.trigamma.block.ModBlocks;
import net.ds.trigamma.block.entity.UniversalMatterDuctBlockEntity;
import net.ds.trigamma.item.ModItems;
import net.ds.trigamma.item.IdentifierTabletItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = TriGamma.MODID, value = Dist.CLIENT)
public class ClientColorRegistry {

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof UniversalMatterDuctBlockEntity duct) {
                    // Look at the locked filter type instead of the active tank volume
                    return duct.getFilterMatter()
                            .map(matter -> matter.color())
                            .orElse(0xFFFFFF); // Default gray/white when unconfigured
                }
            }
            return 0xFFFFFF;
        }, ModBlocks.UNIVERSAL_MATTER_DUCT.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            // Tint index 0 = Tablet Frame (No Tint)
            if (tintIndex == 0) return 0xFFFFFFFF;

            // Tint index 1 = Droplet Overlay
            if (tintIndex == 1 && stack.getItem() instanceof IdentifierTabletItem tablet) {
                return tablet.getSelectedMatter(stack)
                        .map(matter -> {
                            int baseColor = matter.color();
                            // If the color doesn't have an alpha channel already, force it to 100% opaque
                            if ((baseColor & 0xFF000000) == 0) {
                                return baseColor | 0xFF000000;
                            }
                            return baseColor;
                        })
                        // IF EMPTY: Return 0x00FFFFFF (00 Alpha means 100% transparent/invisible!)
                        .orElse(0x00FFFFFF);
            }
            return 0xFFFFFFFF;
        }, ModItems.IDENTIFIER_TABLET.get());
    }
}