package net.ds.trigamma.radiation;

import net.ds.trigamma.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles vomiting behaviour tied to radiation sickness stages.
 *
 * ── Vomit (green)  ── triggered at WEAKNESS threshold (effective dose ≥ 350)
 *   • Occurs every 10–20 s (randomised per-player)
 *   • Green particles spray from the player's head in their look direction
 *   • Applies Hunger II for 3 s
 *   • Visible to ALL players (spawned server-side on ServerLevel)
 *
 * ── Blood Vomit (red) ── triggered at LETHAL threshold (effective dose ≥ 1000)
 *   • Same timing and particle arc as above, but red
 *   • Additionally deals 6 hp (3 hearts) of direct damage
 *
 * Register in your main mod class alongside RadiationEvents:
 *   NeoForge.EVENT_BUS.register(new VomitEvents());
 */
public class VomitEvents {

    // ── Thresholds (must match RadiationEvents) ───────────────────────────────
    private static final float THRESHOLD_VOMIT      = 350f;   // weakness phase
    private static final float THRESHOLD_BLOOD_VOMIT = 1000f; // lethal phase

    // ── Timing ────────────────────────────────────────────────────────────────
    /** Minimum ticks between vomit events (10 s). */
    private static final int VOMIT_COOLDOWN_MIN = 200;
    /** Maximum ticks between vomit events (20 s). */
    private static final int VOMIT_COOLDOWN_MAX = 400;

    // ── Particle configuration ────────────────────────────────────────────────
    /** Number of particles per vomit burst. */
    private static final int PARTICLE_COUNT = 30;
    /** How far in front of the face the spray fans out (blocks). */
    private static final double SPRAY_SPREAD = 0.35;
    /** Particle speed multiplier. */
    private static final double PARTICLE_SPEED = 0.25;

    // ── Per-player state ─────────────────────────────────────────────────────
    /**
     * Stores the tick at which a player will next vomit.
     * Key = player UUID, Value = tickCount target.
     */
    private final Map<UUID, Integer> nextVomitTick = new HashMap<>();

    // ── Event handler ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        RadiationCapability cap = RadiationCapability.get(player);
        float dose = cap.getEffectiveDose();

        // Nothing to do below the vomit threshold
        if (dose < THRESHOLD_VOMIT) {
            nextVomitTick.remove(player.getUUID());
            return;
        }

        int currentTick = player.tickCount;
        UUID id = player.getUUID();

        // Schedule the first vomit for this session if not already queued
        nextVomitTick.computeIfAbsent(id, k -> currentTick + randomCooldown(player));

        if (currentTick >= nextVomitTick.get(id)) {
            boolean isBloodVomit = dose >= THRESHOLD_BLOOD_VOMIT;
            triggerVomit(serverPlayer, isBloodVomit);
            // Schedule next event
            nextVomitTick.put(id, currentTick + randomCooldown(player));
        }
    }

    // ── Core vomit logic ──────────────────────────────────────────────────────

    /**
     * Plays a vomit burst for the player.
     * Particles are sent to all players within tracking range via ServerLevel.
     *
     * @param player       the affected player
     * @param bloodVomit   true → red particles + 3-heart damage; false → green particles
     */
    private void triggerVomit(ServerPlayer player, boolean bloodVomit) {
        ServerLevel level = (ServerLevel) player.level();

        // Spawn point: just in front of and slightly below the eye
        Vec3 eye    = player.getEyePosition();
        Vec3 look   = player.getLookAngle();
        Vec3 origin = eye.add(look.scale(0.5));

        // ── Particles ──────────────────────────────────────────────────────
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Fan the spray around the look vector with some random scatter
            double spreadX = (player.getRandom().nextDouble() - 0.5) * SPRAY_SPREAD;
            double spreadY = (player.getRandom().nextDouble() - 0.5) * SPRAY_SPREAD * 0.6; // less vertical spread
            double spreadZ = (player.getRandom().nextDouble() - 0.5) * SPRAY_SPREAD;

            Vec3 velocity = look
                    .add(spreadX, spreadY, spreadZ)
                    .normalize()
                    .scale(PARTICLE_SPEED + player.getRandom().nextDouble() * 0.1);

            // sendParticles(type, alwaysRender, x, y, z, count, dx, dy, dz, speed)
            // We pass count=1 and dx/dy/dz as the velocity so each particle is individually aimed.
            level.sendParticles(
                    bloodVomit ? ModParticles.BLOOD_VOMIT.get() : ModParticles.VOMIT.get(),
                    origin.x, origin.y, origin.z,
                    1,                        // count (1 so we control velocity per-call)
                    velocity.x, velocity.y, velocity.z,
                    0.0                       // speed (0 → use dx/dy/dz as exact velocity)
            );
        }

        // ── Hunger effect ──────────────────────────────────────────────────
        // Hunger X for 6 seconds (120 ticks)
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 10, false, true));

        // ── Blood-vomit damage ─────────────────────────────────────────────
        if (bloodVomit) {
            DamageSource src = level.damageSources().magic();
            player.hurt(src, 6f); // 6 HP = 3 hearts
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns a random cooldown in ticks between VOMIT_COOLDOWN_MIN and MAX. */
    private int randomCooldown(Player player) {
        return VOMIT_COOLDOWN_MIN
                + player.getRandom().nextInt(VOMIT_COOLDOWN_MAX - VOMIT_COOLDOWN_MIN + 1);
    }
}