package net.ds.trigamma.radiation;

import net.ds.trigamma.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Applies environmental radiation to non-player, non-undead LivingEntities.
 *
 * Uses the same Fibonacci-sphere raycast approach as RadiationEvents for players,
 * but with a smaller ray count for performance (mobs are numerous).
 *
 * ── Immunity rules ────────────────────────────────────────────────────────────
 *   • Undead mobs (MobType.UNDEAD)  → completely immune
 *   • Ender Dragon / Wither          → immune (boss flag)
 *   • Arthropods, Illagers, etc.     → affected normally (they're still biological)
 *
 * ── Thresholds ────────────────────────────────────────────────────────────────
 *   ≥ 100  rads → Slowness I
 *   ≥ 250  rads → Weakness I + Slowness II
 *   ≥ 500  rads → Radiation damage (scales with severity, capped at 2 hearts/s)
 *   ≥ 800  rads → Glowing effect
 *
 * Register in your main mod class:
 *   NeoForge.EVENT_BUS.register(new RadiationMobEvents());
 */
public class RadiationMobEvents {

    // ── Sampling config ───────────────────────────────────────────────────────

    /** Tick interval for mob radiation updates (same 1 s cadence as players). */
    private static final int TICK_RATE = 20;

    /**
     * Fewer rays than players for performance — mobs can be numerous.
     * 12 rays gives a good approximation without being expensive.
     */
    private static final int RAY_COUNT = 12;

    /** Max ray distance in blocks — shorter than player range intentionally. */
    private static final float RAY_DISTANCE = 48f;

    /** How much environmental rads translate to absorbed dose per second. */
    private static final float EXPOSURE_SCALE = 0.08f;

    // ── Effect thresholds (absorbed dose in rads) ─────────────────────────────
    private static final float THRESHOLD_SLOWNESS  = 100f;
    private static final float THRESHOLD_WEAKNESS  = 250f;
    private static final float THRESHOLD_DAMAGE    = 500f;
    private static final float THRESHOLD_GLOWING   = 800f;

    // ── Event handler ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onLivingTick(LivingBreatheEvent event) {
        LivingEntity entity = event.getEntity();

        // ── Skip players (handled by RadiationEvents) ──
        if (entity instanceof net.minecraft.world.entity.player.Player) return;

        // ── Skip client side ──
        if (entity.level().isClientSide()) return;

        // ── Skip immune entities ──
        if (isImmune(entity)) return;

        // ── Only update every TICK_RATE ticks ──
        if (entity.tickCount % TICK_RATE != 0) return;

        MobRadiationCapability cap = MobRadiationCapability.get(entity);

        // 1. Natural dose decay
        cap.tick();

        // 2. Sample environmental radiation
        float envRads = sampleEnvironmentalRadiation(entity);

        // 3. Accumulate absorbed dose
        if (envRads > 0f) {
            cap.addDose(envRads * EXPOSURE_SCALE);
        }

        // 4. Apply biological effects
        applyRadiationEffects(entity, cap);
    }

    // ── Immunity check ────────────────────────────────────────────────────────

    /**
     * Returns true if this entity should be completely immune to radiation.
     *
     *   - Undead mobs: skeletons, zombies, phantoms, withers, etc.
     *   - Ender Dragon: treated as a special case (not undead by MobType but immune)
     *   - Purely mechanical/magical constructs: Iron Golem, Snow Golem, Shulker, Vex
     *     are left to the designer's discretion — currently they ARE affected since
     *     they are living entities with health pools. Uncomment lines below to exempt.
     */
    private boolean isImmune(LivingEntity entity) {
        // Undead: zombies, skeletons, phantoms, withers, zombie piglins, etc.
        if (entity.getType().is(EntityTypeTags.UNDEAD)) return true;

        // Ender Dragon (not flagged as undead but logically immune)
        if (entity.getType() == EntityType.ENDER_DRAGON) return true;

        // Wither (boss, undead — already caught above, but explicit for clarity)
        if (entity.getType() == EntityType.WITHER) return true;

        // Optional: uncomment to exempt constructs
        // if (entity.getType() == EntityType.IRON_GOLEM)  return true;
        // if (entity.getType() == EntityType.SNOW_GOLEM)  return true;
        // if (entity.getType() == EntityType.SHULKER)     return true;
        // if (entity.getType() == EntityType.VEX)         return true;

        return false;
    }

    // ── Environmental radiation sampling ──────────────────────────────────────

    /**
     * Fibonacci-sphere raycast around the entity's eye position.
     * Mirrors the player sampling in RadiationEvents but uses fewer rays.
     */
    private float sampleEnvironmentalRadiation(LivingEntity entity) {
        Vec3 origin = entity.getEyePosition();
        float totalRads = 0f;

        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));

        for (int i = 0; i < RAY_COUNT; i++) {
            double y      = 1.0 - (i / (double)(RAY_COUNT - 1)) * 2.0;
            double radius = Math.sqrt(1.0 - y * y);
            double theta  = goldenAngle * i;

            Vec3 dir = new Vec3(
                    Math.cos(theta) * radius,
                    y,
                    Math.sin(theta) * radius
            ).normalize();

            Vec3 end = origin.add(dir.scale(RAY_DISTANCE));

            BlockHitResult hit = entity.level().clip(new ClipContext(
                    origin, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    entity
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = hit.getBlockPos();
                BlockState state = entity.level().getBlockState(hitPos);
                float blockRads = getBlockRadiation(state);

                if (blockRads > 0f) {
                    double dist = hit.getLocation().distanceTo(origin);
                    float attenuated = blockRads / (float) Math.max(1.0, dist * dist);
                    totalRads += attenuated;
                }
            }
        }

        // Add chunk background radiation
        if (entity.level() instanceof ServerLevel serverLevel) {
            RadiationManager mgr = RadiationManager.get(serverLevel);
            totalRads += mgr.getRadiation(entity.blockPosition()) * 0.01f;
        }

        return totalRads;
    }

    /**
     * Returns radiation emission (rads/s) for a block.
     * Keep in sync with RadiationEvents#getBlockRadiation.
     */
    private float getBlockRadiation(BlockState state) {
        if (state.is(Blocks.ANCIENT_DEBRIS))                          return 1f;
        if (state.is(ModBlocks.NATURAL_URANIUM_BLOCK.get()))          return 1.25f;
        return 0f;
    }

    // ── Biological effects ────────────────────────────────────────────────────

    private void applyRadiationEffects(LivingEntity entity, MobRadiationCapability cap) {
        float dose = cap.getAbsorbedDose();

        if (dose >= THRESHOLD_SLOWNESS) {
            entity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, TICK_RATE * 3, 0, false, false));
        }

        if (dose >= THRESHOLD_WEAKNESS) {
            entity.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, TICK_RATE * 3, 0, false, false));
            entity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, TICK_RATE * 3, 1, false, false)); // upgrade to Slowness II
        }

        if (dose >= THRESHOLD_DAMAGE) {
            DamageSource radiationDmg = entity.level().damageSources().magic();
            // Scales from 0 at 500 rads up to 4 HP/s at 1000 rads
            float dmgAmount = (dose - THRESHOLD_DAMAGE) / 500f * 4f;
            entity.hurt(radiationDmg, Math.min(dmgAmount, 4f));
        }

        if (dose >= THRESHOLD_GLOWING) {
            entity.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING, TICK_RATE * 3, 0, false, false));
        }
    }
}