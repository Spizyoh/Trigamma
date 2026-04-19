package net.ds.trigamma.item;

import net.ds.trigamma.radiation.RadiationSyncPacket.ClientRadiationData;
import net.ds.trigamma.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

public class GeigerCounterItem extends Item {

    /** How many ticks between the fastest possible click (at max radiation). */
    private static final int MIN_CLICK_INTERVAL = 2;   // 2 ticks  = very rapid
    /** How many ticks between clicks at zero radiation. */
    private static final int MAX_CLICK_INTERVAL = 80;  // 4 seconds = rare background clicks

    /** Radiation level (effective dose) at which the counter maxes out its click rate. */
    private static final float MAX_RADS_FOR_SCALE = 600f;

    public GeigerCounterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!(entity instanceof Player player)) return;

        // Play sounds client-side whenever the item is anywhere in the inventory
        if (!level.isClientSide) return;

        float dose = ClientRadiationData.getEffectiveDose();

        // Calculate click interval: linearly interpolate between MAX and MIN
        float t = Math.min(dose / MAX_RADS_FOR_SCALE, 1f);
        int interval = (int)(MAX_CLICK_INTERVAL - t * (MAX_CLICK_INTERVAL - MIN_CLICK_INTERVAL));

        if (player.tickCount % interval == 0) {
            float pitch = 0.8f + level.getRandom().nextFloat() * 0.4f;
            level.playLocalSound(
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.GEIGER_CLICK.get(),
                    SoundSource.PLAYERS,
                    0.3f,
                    pitch,
                    false
            );
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Detects nearby radiation sources.")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Keep in inventory to monitor radiation levels.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}