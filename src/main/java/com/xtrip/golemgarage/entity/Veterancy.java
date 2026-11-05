package com.xtrip.golemgarage.entity;

import com.xtrip.golemgarage.golem.GolemStats;

import java.util.EnumSet;
import java.util.Set;

/**
 * Golem veterancy. Golems earn experience from work and fights and climb five ranks, each a little tougher and
 * harder-hitting. At Veteran they pick up a trait from how they've spent their life (fighters become Brawlers,
 * workers become Tireless), and Legends become Unbreakable.
 */
public final class Veterancy {
    public static final int[] THRESHOLDS = {0, 100, 300, 700, 1500};
    private static final String[] RANKS = {"rookie", "seasoned", "veteran", "elite", "legend"};

    public enum Trait {
        /** +15% damage. Golems that mostly fought. */
        BRAWLER,
        /** Uses a third less power. Golems that mostly worked. */
        TIRELESS,
        /** Parts wear half as fast. Every Legend. */
        UNBREAKABLE
    }

    private Veterancy() {
    }

    public static int rank(int xp) {
        int r = 0;
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (xp >= THRESHOLDS[i]) {
                r = i;
            }
        }
        return r;
    }

    public static String rankKey(int rank) {
        return "golemgarage.rank." + RANKS[Math.max(0, Math.min(RANKS.length - 1, rank))];
    }

    /** Experience needed for the next rank, or -1 at Legend. */
    public static int next(int xp) {
        int r = rank(xp);
        return r + 1 < THRESHOLDS.length ? THRESHOLDS[r + 1] : -1;
    }

    public static Set<Trait> traits(GolemEntity golem) {
        Set<Trait> out = EnumSet.noneOf(Trait.class);
        int r = golem.rank();
        if (r >= 2) {
            out.add(golem.combatXp() * 2 >= golem.getXp() ? Trait.BRAWLER : Trait.TIRELESS);
        }
        if (r >= 4) {
            out.add(Trait.UNBREAKABLE);
        }
        return out;
    }

    public static String traitKey(Trait t) {
        return "golemgarage.trait." + t.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** +5% health and damage per rank, and the Brawler bonus. */
    static void apply(GolemEntity golem, GolemStats stats) {
        int r = golem.rank();
        stats.maxHealth *= 1f + 0.05f * r;
        stats.attack *= 1f + 0.05f * r;
        if (golem.hasTrait(Trait.BRAWLER)) {
            stats.attack *= 1.15f;
        }
    }
}
