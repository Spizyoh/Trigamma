package net.ds.trigamma.client;

import net.ds.trigamma.radiation.RadioactiveItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Adds a radiation tooltip to any item registered as radioactive.
 *
 * Tooltip format (single item):
 *   ☢ Radioactive (5.0 rads/s)
 *
 * Tooltip format (stacked item):
 *   ☢ Radioactive (5.0 rads/s × 32 = 160.0 rads/s)
 *
 * Register this class on the MOD event bus (client-only):
 * <pre>{@code
 *   // In your client-only setup:
 *   NeoForge.EVENT_BUS.register(new RadiationItemTooltip());
 * }</pre>
 */
@OnlyIn(Dist.CLIENT)
public class RadiationItemTooltip {

    /** Colour used for the radioactive tooltip line. */
    private static final ChatFormatting COLOUR = ChatFormatting.GREEN;

    /** Radioactive symbol prefix. */
    private static final String SYMBOL = "☢ ";

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        float baseRads = RadioactiveItemRegistry.getBaseRadiation(stack.getItem());
        if (baseRads <= 0f) return;

        int count = stack.getCount();

        Component line;
        if (count <= 1) {
            // Simple: ☢ Radioactive (5.0 rads/s)
            line = Component.literal(SYMBOL + "Radioactive")
                    .withStyle(COLOUR)
                    .append(Component.literal(
                            String.format(" (%.1f rads/s)", baseRads)
                    ).withStyle(ChatFormatting.GRAY));
        } else {
            // Stacked: ☢ Radioactive (5.0 rads/s × 32 = 160.0 rads/s)
            float totalRads = baseRads * count;
            line = Component.literal(SYMBOL + "Radioactive")
                    .withStyle(COLOUR)
                    .append(Component.literal(
                            String.format(" (%.1f rads/s × %d = %.1f rads/s)", baseRads, count, totalRads)
                    ).withStyle(ChatFormatting.GRAY));
        }

        // Insert after the item name (index 1), before lore/enchantments
        event.getToolTip().add(1, line);
    }
}