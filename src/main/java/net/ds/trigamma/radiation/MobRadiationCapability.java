package net.ds.trigamma.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.function.Supplier;

/**
 * Stores radiation contamination on any non-player LivingEntity.
 *
 * Simpler than RadiationCapability — mobs only track a single "absorbed dose"
 * value rather than external/internal split, since we don't need wash mechanics
 * or HUD syncing for mobs.
 *
 * Dose ranges and thresholds (rads):
 *   ≥ 100  → Slowness I
 *   ≥ 250  → Weakness I + Slowness II
 *   ≥ 500  → Ongoing damage (scales with dose)
 *   ≥ 800  → Glowing (eerie visual marker)
 *
 * Undead mobs are immune (they're already dead).
 * Bosses (withers, ender dragons) are immune by flag.
 *
 * Register the ATTACHMENT_TYPES deferred register on your mod event bus:
 *   MobRadiationCapability.ATTACHMENT_TYPES.register(modBus);
 */
public class MobRadiationCapability {

    // ── Codec ─────────────────────────────────────────────────────────────────
    public static final Codec<MobRadiationCapability> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("absorbed_dose").forGetter(c -> c.absorbedDose)
    ).apply(inst, dose -> {
        MobRadiationCapability cap = new MobRadiationCapability();
        cap.absorbedDose = dose;
        return cap;
    }));

    // ── Attachment registration ───────────────────────────────────────────────
    // NOTE: We reuse RadiationCapability.ATTACHMENT_TYPES rather than creating
    // a second DeferredRegister for the same "trigamma" namespace. Both player
    // and mob attachments live on the same register, registered once on the mod bus.
    public static final Supplier<AttachmentType<MobRadiationCapability>> MOB_RADIATION =
            RadiationCapability.ATTACHMENT_TYPES.register("mob_radiation", () ->
                    AttachmentType.builder(MobRadiationCapability::new)
                            .serialize(CODEC)
                            .build());

    // ── Fields ────────────────────────────────────────────────────────────────
    /** Total absorbed dose, 0–1000 rads. */
    private float absorbedDose = 0f;

    public MobRadiationCapability() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public float getAbsorbedDose() { return absorbedDose; }

    // ── Mutation ──────────────────────────────────────────────────────────────
    public void addDose(float amount) {
        absorbedDose = Math.max(0f, Math.min(1000f, absorbedDose + amount));
    }

    /**
     * Natural decay — mobs process radiation a bit faster than players
     * since they have no clothing or internal contamination distinction.
     * Called once per second by RadiationMobEvents.
     */
    public void tick() {
        // ~0.1 rads/s decay — slower than external player decay but still gradual
        absorbedDose = Math.max(0f, absorbedDose - 0.1f);
    }

    // ── Convenience static accessor ───────────────────────────────────────────
    public static MobRadiationCapability get(LivingEntity entity) {
        return entity.getData(MOB_RADIATION.get());
    }
}