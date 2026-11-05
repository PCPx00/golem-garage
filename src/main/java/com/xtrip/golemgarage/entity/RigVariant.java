package com.xtrip.golemgarage.entity;

import com.xtrip.golemgarage.item.ModItems;
import net.minecraft.world.item.Item;

import java.util.Locale;

/**
 * The four rigs you can build. Each has its own look, handling and toughness:
 * <ul>
 *   <li>War Rig: the all-rounder.</li>
 *   <li>Interceptor: fast and light, with a nitro boost (hold jump, let go to fire it).</li>
 *   <li>Juggernaut: a slow tracked tank with thick armour, a heavy ram and a cannon that shells hostile mobs.</li>
 *   <li>Swamp Skimmer: a hovercraft with a big fan on the back. Rides over water as easily as land.</li>
 * </ul>
 */
public enum RigVariant {
    WAR_RIG(80, 0.34, 8, 1.05, 1.0f),
    INTERCEPTOR(56, 0.46, 4, 1.05, 0.8f),
    JUGGERNAUT(180, 0.24, 16, 1.6, 1.7f),
    SKIMMER(70, 0.36, 6, 1.05, 0.8f);

    public final double health, speed, armor, step;
    public final float ram;

    RigVariant(double health, double speed, double armor, double step, float ram) {
        this.health = health;
        this.speed = speed;
        this.armor = armor;
        this.step = step;
        this.ram = ram;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Item item() {
        return switch (this) {
            case WAR_RIG -> ModItems.RIG.get();
            case INTERCEPTOR -> ModItems.RIG_INTERCEPTOR.get();
            case JUGGERNAUT -> ModItems.RIG_JUGGERNAUT.get();
            case SKIMMER -> ModItems.RIG_SKIMMER.get();
        };
    }

    public static RigVariant of(int i) {
        return values()[Math.floorMod(i, values().length)];
    }
}
