package com.xtrip.golemgarage.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** A small "z" that drifts up and sideways, grows, and fades. */
public class SleepParticle extends TextureSheetParticle {
    private final float startSize;

    protected SleepParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 40;
        this.gravity = 0;
        this.xd = 0.008;
        this.yd = 0.02;
        this.zd = 0;
        this.startSize = 0.08f;
        this.quadSize = startSize;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        super.tick();
        float t = age / (float) lifetime;
        this.quadSize = startSize * (1f + t);
        this.alpha = 1f - t;
        this.xd = 0.01 * Mth.sin(age * 0.3f);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            SleepParticle p = new SleepParticle(level, x, y, z);
            p.pickSprite(sprites);
            return p;
        }
    }
}
