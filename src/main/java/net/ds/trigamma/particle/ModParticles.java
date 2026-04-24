package net.ds.trigamma.particle;

import net.ds.trigamma.TriGamma;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registers custom particle types for the radiation mod.
 *
 * ── Registration ──────────────────────────────────────────────────────────────
 * In your main mod class (TriGamma), call:
 *
 *   ModParticles.PARTICLE_TYPES.register(modBus);
 *
 * ── Client-side textures ──────────────────────────────────────────────────────
 * Each particle type needs a JSON descriptor and a texture:
 *
 * See VomitParticle.java for the client-side rendering provider.
 * See ClientSetup.java snippet below for how to register the factory.
 */
public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, TriGamma.MODID);

    /**
     * Green vomit — shown during the weakness/nausea radiation phase.
     * overrideLimiter = true so the particle always renders regardless of particle settings.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOMIT =
            PARTICLE_TYPES.register("vomit", () -> new SimpleParticleType(true));

    /**
     * Red blood vomit — shown at lethal radiation dose.
     * overrideLimiter = true for the same reason.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_VOMIT =
            PARTICLE_TYPES.register("blood_vomit", () -> new SimpleParticleType(true));
}

/*
 * ═══════════════════════════════════════════════════════════════════════════════
 * HOW TO WIRE EVERYTHING UP  (copy-paste into the relevant classes)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * ── 1. TriGamma.java (main mod class, modBus constructor) ─────────────────────
 *
 *   ModParticles.PARTICLE_TYPES.register(modBus);
 *   modBus.register(new VomitEvents());            // common event bus registration
 *   // or if you use NeoForge.EVENT_BUS:
 *   NeoForge.EVENT_BUS.register(new VomitEvents());
 *
 * ── 2. ClientSetup.java (or your @Mod.EventBusSubscriber client class) ─────────
 *
 *   @SubscribeEvent
 *   public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
 *       event.registerSpriteSet(ModParticles.VOMIT.get(),       VomitParticle.GreenProvider::new);
 *       event.registerSpriteSet(ModParticles.BLOOD_VOMIT.get(), VomitParticle.RedProvider::new);
 *   }
 *
 * ── 3. Particle JSON files (create both, same format) ────────────────────────
 *
 *   src/main/resources/assets/trigamma/particles/vomit.json:
 *   {
 *     "textures": [ "trigamma:vomit" ]
 *   }
 *
 *   src/main/resources/assets/trigamma/particles/blood_vomit.json:
 *   {
 *     "textures": [ "trigamma:blood_vomit" ]
 *   }
 *
 * ── 4. Textures ──────────────────────────────────────────────────────────────
 *   Place 8×8 or 16×16 PNG textures at:
 *   src/main/resources/assets/trigamma/textures/particle/vomit.png       (green blob)
 *   src/main/resources/assets/trigamma/textures/particle/blood_vomit.png (red blob)
 *
 *   Tip: copy vanilla's "splash" particle texture and recolor it.
 *   Vanilla splash lives at: assets/minecraft/textures/particle/splash.png
 * ═══════════════════════════════════════════════════════════════════════════════
 */