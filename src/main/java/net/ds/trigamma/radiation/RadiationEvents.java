package net.ds.trigamma.radiation;

import net.ds.trigamma.TriGamma;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Core radiation logic wired to NeoForge events.
 *
 * Register this class as a common event listener in your main mod class:
 *   NeoForge.EVENT_BUS.register(new RadiationEvents());
 */
public class RadiationEvents {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** How often (in ticks) we run the full raycast + contamination update. */
    private static final int TICK_RATE = 20; // once per second

    /** Number of rays cast in the sphere around the player. More = more accurate, more expensive. */
    private static final int RAY_COUNT = 32;

    /** Maximum distance (blocks) a radiation ray travels. */
    private static final float RAY_DISTANCE = 16f;

    /** Rad dose absorbed per second when fully exposed to chunk radiation. */
    private static final float EXPOSURE_SCALE = 0.1f;

    // Damage thresholds (effective dose in rads):
    private static final float THRESHOLD_NAUSEA    = 150f;
    private static final float THRESHOLD_WEAKNESS  = 350f;
    private static final float THRESHOLD_DAMAGE    = 700f;
    private static final float THRESHOLD_LETHAL    = 1000f;

    // ── Server-side player tick ───────────────────────────────────────────────

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        // Only run on server, only every TICK_RATE ticks
        if (player.level().isClientSide) return;
        if (player.tickCount % TICK_RATE != 0) return;

        RadiationCapability cap = RadiationCapability.get(player);

        // 1. Decay contamination naturally
        cap.tick();

        // 2. Sample environmental radiation via raycasting
        float envRads = sampleEnvironmentalRadiation(player);

        // 3. Add external contamination from environment
        if (envRads > 0) {
            cap.addExternalRads(envRads * EXPOSURE_SCALE);
        }

        // 4. Apply biological effects based on effective dose
        applyRadiationEffects(player, cap);

        // 5. Sync to client for HUD
        syncToClient((ServerPlayer) player, cap);
    }

    // ── Raycast environmental radiation sampling ───────────────────────────────

    /**
     * Casts rays in a Fibonacci sphere around the player.
     * Each ray accumulates rads from radioactive blocks it hits,
     * attenuated by shielding materials and distance.
     *
     * @return total rads/s the player is currently exposed to
     */
    private float sampleEnvironmentalRadiation(Player player) {
        Vec3 origin = player.getEyePosition();
        float totalRads = 0f;

        // Golden-angle Fibonacci sphere for uniform ray distribution
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));

        for (int i = 0; i < RAY_COUNT; i++) {
            double y = 1.0 - (i / (double)(RAY_COUNT - 1)) * 2.0;
            double radius = Math.sqrt(1.0 - y * y);
            double theta = goldenAngle * i;

            Vec3 dir = new Vec3(
                    Math.cos(theta) * radius,
                    y,
                    Math.sin(theta) * radius
            ).normalize();

            Vec3 end = origin.add(dir.scale(RAY_DISTANCE));

            BlockHitResult hit = player.level().clip(new ClipContext(
                    origin, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = hit.getBlockPos();
                BlockState state = player.level().getBlockState(hitPos);
                float blockRads = getBlockRadiation(state);
                if (blockRads > 0) {
                    float dist = (float) hit.getLocation().distanceTo(origin);
                    // Inverse-square falloff, minimum 1 to avoid division by zero
                    float attenuated = blockRads / Math.max(1f, dist * dist * 0.1f);
                    totalRads += attenuated;
                }
            }
        }

        // Also factor in chunk-level background radiation
        if (player.level() instanceof ServerLevel serverLevel) {
            RadiationManager mgr = RadiationManager.get(serverLevel);
            totalRads += mgr.getRadiation(player.blockPosition()) * 0.01f;
        }

        return totalRads / RAY_COUNT * RAY_COUNT; // normalise across ray count
    }

    /**
     * Returns the radiation emission (rads/s) of a given block.
     * Extend this with your own radioactive blocks!
     */
    private float getBlockRadiation(BlockState state) {
        // ── Built-in vanilla blocks ──
        if (state.is(Blocks.ANCIENT_DEBRIS))    return 15f;
        if (state.is(Blocks.NETHER_GOLD_ORE))   return 5f;

        // ── Your mod blocks ──
        // if (state.is(ModBlocks.URANIUM_ORE.get()))  return 80f;
        // if (state.is(ModBlocks.REACTOR_CORE.get())) return 500f;

        return 0f;
    }

    // ── Biological effects ────────────────────────────────────────────────────

    private void applyRadiationEffects(Player player, RadiationCapability cap) {
        float dose = cap.getEffectiveDose();

        if (dose >= THRESHOLD_NAUSEA) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, TICK_RATE * 3, 0, false, false));
        }

        if (dose >= THRESHOLD_WEAKNESS) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, TICK_RATE * 3, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, TICK_RATE * 3, 1, false, false));
        }

        if (dose >= THRESHOLD_DAMAGE) {
            // Radiation sickness: direct health damage every tick cycle
            DamageSource radiationDmg = player.level().damageSources().magic();
            float dmgAmount = (dose - THRESHOLD_DAMAGE) / 1000f; // scales with severity
            player.hurt(radiationDmg, Math.min(dmgAmount, 4f)); // cap at 2 hearts/s
        }

        if (dose >= THRESHOLD_LETHAL) {
            // Glowing effect at lethal dose (eerie aesthetic)
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, TICK_RATE * 3, 0, false, false));
        }
    }

    // ── Player clone (respawn) ────────────────────────────────────────────────

    /**
     * When a player dies and respawns, copy contamination to the new entity.
     * Internal contamination persists through death; external is halved (clothes gone).
     */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        RadiationCapability oldCap = RadiationCapability.get(event.getOriginal());
        RadiationCapability newCap = RadiationCapability.get(event.getEntity());

        newCap.addInternalRads(oldCap.getInternalRads()); // internal persists fully
        newCap.addExternalRads(oldCap.getExternalRads() * 0.5f); // external halved on respawn
    }

    // ── Client sync ───────────────────────────────────────────────────────────

    /**
     * Send updated radiation data to the client for HUD rendering.
     * Uses the RadiationSyncPacket defined in the network package.
     */
    private void syncToClient(ServerPlayer player, RadiationCapability cap) {
        PacketDistributor.sendToPlayer(player, new RadiationSyncPacket(
                cap.getExternalRads(),
                cap.getInternalRads()
        ));
    }
}