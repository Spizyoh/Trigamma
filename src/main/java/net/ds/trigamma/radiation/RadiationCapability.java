package net.ds.trigamma.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Stores per-player radiation contamination.
 *
 *  externalRads  – surface contamination (skin/clothes). Decays faster, removed by washing.
 *  internalRads  – ingested / inhaled contamination. Decays very slowly, causes ongoing damage.
 *
 * Both values are in "rads". The effective dose used for damage is:
 *   effectiveDose = externalRads + internalRads * 2
 */
public class RadiationCapability {

    // ── Codec for persistence ─────────────────────────────────────────────────
    public static final Codec<RadiationCapability> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("external").forGetter(c -> c.externalRads),
            Codec.FLOAT.fieldOf("internal").forGetter(c -> c.internalRads)
    ).apply(inst, (ext, intern) -> {
        RadiationCapability cap = new RadiationCapability();
        cap.externalRads = ext;
        cap.internalRads = intern;
        return cap;
    }));
    // ── Attachment registration ───────────────────────────────────────────────
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "trigamma");

    public static final Supplier<AttachmentType<RadiationCapability>> RADIATION =
            ATTACHMENT_TYPES.register("radiation", () ->
                    AttachmentType.builder(RadiationCapability::new)
                            .serialize(CODEC)
                            .build());
    // ── Fields ────────────────────────────────────────────────────────────────
    private float externalRads = 0f;   // 0 – 1000 range typical
    private float internalRads = 0f;   // 0 – 500  range typical

    public RadiationCapability() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public float getExternalRads() { return externalRads; }
    public float getInternalRads() { return internalRads; }

    /** Combined effective dose (used for HUD display and damage calculation). */
    public float getEffectiveDose() {
        return externalRads + internalRads * 2f;
    }

    // ── Mutation ──────────────────────────────────────────────────────────────
    public void addExternalRads(float amount) {
        externalRads = Math.max(0, Math.min(1000f, externalRads + amount));
    }

    public void addInternalRads(float amount) {
        internalRads = Math.max(0, Math.min(500f, internalRads + amount));
    }

    /** Call once per second on the server tick to decay contamination naturally. */
    public void tick() {
        // External decays at ~0.5 rads/s (half-life ≈ 23 min real-time)
        externalRads = Math.max(0, externalRads - 0.05f);
        // Internal decays at ~0.05 rads/s (much slower, represents true contamination)
        internalRads = Math.max(0, internalRads - 0.005f);
    }

    /** Washing with water removes external contamination (e.g. right-clicking a water source). */
    public void wash() {
        externalRads = Math.max(0, externalRads - 50f);
    }

    // ── Convenience static accessors ─────────────────────────────────────────
    public static RadiationCapability get(Player player) {
        return player.getData(RADIATION.get());
    }
}