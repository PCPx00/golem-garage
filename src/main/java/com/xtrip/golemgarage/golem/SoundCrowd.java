package com.xtrip.golemgarage.golem;

import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps a crowd of golems from getting loud. Each golem sound checks how many nearby golems made a sound of
 * the same kind just now: the more there are, the quieter it plays, and past a few it's skipped. Ten golems
 * walking together sound like a few, not like ten.
 */
public final class SoundCrowd {
    /** Kinds of sound that share a budget. */
    public enum Kind {
        STEP(12, 3, 9.0), WORK(20, 2, 10.0), AMBIENT(200, 1, 14.0), FIGHT(10, 3, 10.0);

        final int window;
        final int max;
        final double radius;

        Kind(int window, int max, double radius) {
            this.window = window;
            this.max = max;
            this.radius = radius;
        }
    }

    private record Played(double x, double y, double z, long time) {
    }

    private static final Map<Object, Deque<Played>> RECENT = new HashMap<>();
    private static int played;
    private static int skipped;
    private static long lastReport;

    private SoundCrowd() {
    }

    /** The volume factor for a golem about to make this kind of sound: 0 means skip it. */
    public static float factor(Entity golem, Kind kind) {
        if (golem.level().isClientSide) {
            return 1f;
        }
        long now = golem.level().getGameTime();
        Deque<Played> q = RECENT.computeIfAbsent(java.util.List.of(golem.level().dimension(), kind), k -> new ArrayDeque<>());
        while (!q.isEmpty() && now - q.peekFirst().time() > kind.window) {
            q.pollFirst();
        }
        int near = 0;
        double r2 = kind.radius * kind.radius;
        for (Played p : q) {
            double dx = p.x() - golem.getX(), dy = p.y() - golem.getY(), dz = p.z() - golem.getZ();
            if (dx * dx + dy * dy + dz * dz < r2) {
                near++;
            }
        }
        if (com.xtrip.golemgarage.entity.GolemEntity.DEBUG && now - lastReport >= 200) {
            com.xtrip.golemgarage.GolemGarage.LOGGER.info("[sounds] last 10s: {} played, {} skipped", played, skipped);
            played = 0;
            skipped = 0;
            lastReport = now;
        }
        if (near >= kind.max) {
            skipped++;
            return 0f;
        }
        played++;
        q.addLast(new Played(golem.getX(), golem.getY(), golem.getZ(), now));
        return 1f / (1f + 0.6f * near);
    }
}
