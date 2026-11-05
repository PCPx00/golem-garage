package com.xtrip.golemgarage.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import com.xtrip.golemgarage.golem.GolemStats;

/**
 * Every golem is born with a personality. It shows in little idle moments and nudges one stat:
 * Cheerful golems walk a touch faster, Grumpy ones shrug off a tenth of the damage, Lazy ones save power but dawdle,
 * Brave ones hit harder and Curious ones look around a lot and move a little quicker.
 */
public enum Personality {
    CHEERFUL(ParticleTypes.NOTE),
    GRUMPY(ParticleTypes.ANGRY_VILLAGER),
    LAZY(ParticleTypes.CLOUD),
    BRAVE(ParticleTypes.CRIT),
    CURIOUS(ParticleTypes.ENCHANT);

    public final ParticleOptions particle;

    Personality(ParticleOptions particle) {
        this.particle = particle;
    }

    public String key() {
        return "golemgarage.personality." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Personality of(int i) {
        return values()[Math.floorMod(i, values().length)];
    }

    static void apply(Personality p, GolemStats stats) {
        switch (p) {
            case CHEERFUL -> stats.speed *= 1.05f;
            case LAZY -> stats.speed *= 0.95f;
            case BRAVE -> stats.attack *= 1.1f;
            case CURIOUS -> stats.speed *= 1.03f;
            default -> {
            }
        }
    }
}
