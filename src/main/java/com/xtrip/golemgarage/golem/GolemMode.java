package com.xtrip.golemgarage.golem;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Simple behavior modes for players who don't want to write rules. Any mode other than RULES overrides the
 * golem's rules completely (the rules are kept, just paused, and come back when the mode is set to Rules).
 */
public enum GolemMode {
    /** Follows its rules (the default). */
    RULES,
    /** Follows you and attacks every hostile mob within 12 blocks. */
    AGGRESSIVE,
    /** Follows you and never attacks anything, even when hit. */
    PEACEFUL,
    /** Follows you and only fights mobs that attack you. */
    GUARD;

    public static final float AGGRO_RANGE = 12f;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("golemgarage.mode." + id());
    }

    public Component help() {
        return Component.translatable("golemgarage.mode." + id() + ".help");
    }

    public static GolemMode byId(int i) {
        GolemMode[] all = values();
        return all[Math.floorMod(i, all.length)];
    }

    public GolemMode next() {
        return byId(ordinal() + 1);
    }
}
