package com.xtrip.golemgarage.particle;

import com.xtrip.golemgarage.GolemGarage;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, GolemGarage.MOD_ID);

    /** The little "z" a sleepy Cub breathes out. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SLEEP_Z =
            PARTICLES.register("sleep_z", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
