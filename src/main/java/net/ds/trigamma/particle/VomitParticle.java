package net.ds.trigamma.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Client-side particle renderer for both vomit variants.
 *
 * Both variants share the same physics (droop with gravity, short lifetime, shrink on death).
 * Color is the only difference: green for VOMIT, red for BLOOD_VOMIT.
 *
 * Register both providers in your client setup:
 *   event.registerSpriteSet(ModParticles.VOMIT.get(),       VomitParticle.GreenProvider::new);
 *   event.registerSpriteSet(ModParticles.BLOOD_VOMIT.get(), VomitParticle.RedProvider::new);
 */
public class VomitParticle extends TextureSheetParticle {

    // ── Physics ───────────────────────────────────────────────────────────────
    private static final float GRAVITY      = 0.04f;  // how fast it droops downward
    private static final int   LIFETIME     = 18;     // ticks before fully faded
    private static final float START_SCALE  = 0.15f;
    private static final float END_SCALE    = 0.05f;

    protected VomitParticle(
            ClientLevel level,
            double x, double y, double z,
            double vx, double vy, double vz,
            SpriteSet sprites,
            float r, float g, float b
    ) {
        super(level, x, y, z, vx, vy, vz);

        // Sprite
        setSpriteFromAge(sprites);
        pickSprite(sprites);

        // Color
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.alpha = 1.0f;

        // Motion
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = GRAVITY;

        // Size & lifetime
        this.quadSize = START_SCALE;
        this.lifetime = LIFETIME;

        // Don't collide with terrain — looks better as a spray arc
        this.hasPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();

        // Shrink particle as it ages
        float ageFraction = (float) age / (float) lifetime;
        this.quadSize = START_SCALE + (END_SCALE - START_SCALE) * ageFraction;

        // Fade out in the last third of life
        if (ageFraction > 0.66f) {
            this.alpha = 1.0f - ((ageFraction - 0.66f) / 0.34f);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        // PARTICLE_SHEET_TRANSLUCENT supports alpha blending (needed for fade-out)
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // ── Green vomit variant ───────────────────────────────────────────────────

    public static class GreenProvider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public GreenProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x, double y, double z,
                double vx, double vy, double vz
        ) {
            // Slightly varied green shades for visual interest
            float g = 0.6f + (level.random.nextFloat() * 0.3f); // 0.6 – 0.9
            float r = 0.1f + (level.random.nextFloat() * 0.15f);
            float b = 0.05f;
            return new VomitParticle(level, x, y, z, vx, vy, vz, sprites, r, g, b);
        }
    }

    // ── Red blood vomit variant ───────────────────────────────────────────────

    public static class RedProvider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public RedProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x, double y, double z,
                double vx, double vy, double vz
        ) {
            // Deep red with slight variation
            float r = 0.65f + (level.random.nextFloat() * 0.3f); // 0.65 – 0.95
            float g = 0.0f + (level.random.nextFloat() * 0.08f);
            float b = 0.0f + (level.random.nextFloat() * 0.05f);
            return new VomitParticle(level, x, y, z, vx, vy, vz, sprites, r, g, b);
        }
    }
}