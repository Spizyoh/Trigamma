package net.ds.trigamma.client;

import net.ds.trigamma.item.GeigerCounterItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a radiation HUD in the bottom-left of the screen.
 * Only visible when a Geiger Counter is anywhere in the player's inventory.
 *
 * Layout:
 *   ☢ [████░░░░] 142 RAD   ← effective dose bar
 *       EXT: 80   INT: 31   ← breakdown
 */
public class RadiationHudOverlay {

    // Bar dimensions
    private static final int BAR_WIDTH  = 80;
    private static final int BAR_HEIGHT = 6;

    // Thresholds matching RadiationEvents (for colour coding)
    private static final float THRESHOLD_NAUSEA   =  150f;
    private static final float THRESHOLD_DAMAGE   = 700f;
    private static final float THRESHOLD_LETHAL   = 1000f;
    private static final float MAX_DISPLAY        = 1200f;

    public static void renderRadiation(GuiGraphics gfx, DeltaTracker partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        // Only show the HUD if the player has a Geiger Counter somewhere in their inventory
        boolean hasGeigerCounter = false;
        for (ItemStack stack : mc.player.getInventory().items) {
            if (stack.getItem() instanceof GeigerCounterItem) {
                hasGeigerCounter = true;
                break;
            }
        }
        if (!hasGeigerCounter) return;

        float effective = ClientRadiationData.getEffectiveDose();

        int screenH = mc.getWindow().getGuiScaledHeight();
        int x = 10;
        int y = screenH - 60;

        // Background panel
        gfx.fill(x - 2, y - 2, x + BAR_WIDTH + 60, y + BAR_HEIGHT * 2 + 18, 0x88000000);

        // Label
        gfx.drawString(mc.font, "☢ RADIATION", x, y, 0xFF_FF_FF_00, false);
        y += 10;

        // Bar fill
        float fillFraction = Math.min(effective / MAX_DISPLAY, 1f);
        int fillWidth = (int)(BAR_WIDTH * fillFraction);
        int barColor = doseColor(effective);

        gfx.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF_33_33_33); // background
        gfx.fill(x, y, x + fillWidth, y + BAR_HEIGHT, barColor);       // progress

        // Outline
        gfx.renderOutline(x, y, BAR_WIDTH, BAR_HEIGHT, 0xFF_AA_AA_AA);

        // Text values
        String doseText = String.format("%.0f RAD", effective);
        gfx.drawString(mc.font, doseText, x + BAR_WIDTH + 4, y, barColor, false);

        y += BAR_HEIGHT + 4;
        String breakdown = String.format("EXT:%.0f  INT:%.0f",
                ClientRadiationData.getExternalRads(),
                ClientRadiationData.getInternalRads());
        gfx.drawString(mc.font, breakdown, x, y, 0xFF_BB_BB_BB, false);
    }

    private static int doseColor(float dose) {
        if (dose < THRESHOLD_NAUSEA)  return 0xFF_00_FF_44;
        if (dose < THRESHOLD_DAMAGE)  return 0xFF_FF_DD_00;
        if (dose < THRESHOLD_LETHAL)  return 0xFF_FF_77_00;
        return 0xFF_FF_22_22;
    }
}