package com.xtrip.golemgarage.golem;

import net.minecraft.network.chat.Component;

/**
 * Status and log lines are made on the server but read on each client, so they travel as a
 * translation key plus arguments ("@key|arg|arg") and are translated in the reader's language.
 */
public final class GolemText {
    private static final String MARK = "@";
    private static final String SEP = "|";
    private static final String NESTED = "\u0002";

    private GolemText() {
    }

    /** Packs a translatable line. Arguments that are themselves packed lines are translated too. */
    public static String of(String key, Object... args) {
        StringBuilder b = new StringBuilder(MARK).append("golemgarage.status.").append(key);
        for (Object a : args) {
            // A packed line used as an argument keeps its own separators under a different mark.
            b.append(SEP).append(String.valueOf(a).replace(SEP, NESTED));
        }
        return b.toString();
    }

    /** Turns a stored line back into text for display. Plain (old or literal) lines show as they are. */
    public static Component show(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.translatable("golemgarage.status.idle");
        }
        if (!raw.startsWith(MARK)) {
            return Component.literal(raw);
        }
        String[] parts = raw.substring(1).split("\\|", -1);
        Object[] args = new Object[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            String a = parts[i];
            if (a.startsWith("#")) {
                args[i - 1] = Component.translatable(a.substring(1));
            } else if (a.startsWith(MARK)) {
                args[i - 1] = show(a.replace(NESTED, SEP));
            } else {
                args[i - 1] = a;
            }
        }
        return Component.translatable(parts[0], args);
    }

    /** A translatable argument: shown translated inside the line (e.g. an item or block name key). */
    public static String key(String translationKey) {
        return "#" + translationKey;
    }

    /** An entity's name as an argument: its custom name if it has one, otherwise its translated type name. */
    public static String name(net.minecraft.world.entity.Entity e) {
        if (e.hasCustomName() && e.getCustomName() != null) {
            return e.getCustomName().getString();
        }
        return key(e.getType().getDescriptionId());
    }

    /** True when a line means the golem can't carry on by itself (shown in red, with the stuck icon). */
    public static boolean isProblem(String raw) {
        return raw != null && (raw.startsWith(MARK + "golemgarage.status.stuck")
                || raw.startsWith(MARK + "golemgarage.status.stopped")
                || raw.startsWith("Stuck") || raw.startsWith("Stopped"));
    }
}
