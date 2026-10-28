package com.xtrip.golemgarage.golem;

import org.jetbrains.annotations.Nullable;

/** Upgrade modules. A golem has two module sockets; each module adds one effect. */
public enum GolemModule {
    MAGNET("magnet", 0x6FB8FF),
    LAMP("lamp", 0xFFE27A),
    SPEED("speed", 0x7BE08E),
    STORAGE("storage", 0xC69A74),
    PLATING("plating", 0xB8C0CC),
    REGEN("regen", 0xFF7A9C),
    SELF_REPAIR("self_repair", 0xFFB45E);

    public static final int SOCKETS = 2;

    public final String id;
    public final int color;

    GolemModule(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String itemId() {
        return "module_" + id;
    }

    @Nullable
    public static GolemModule byId(String id) {
        for (GolemModule m : values()) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return null;
    }

    /** Adds this module's effect to computed stats. */
    public void apply(GolemStats s) {
        switch (this) {
            case MAGNET -> s.magnet = true;
            case LAMP -> s.lightLevel = 15;
            case SPEED -> s.speed *= 1.2f;
            case STORAGE -> s.carry = Math.min(36, s.carry + 9);
            case PLATING -> {
                s.armor += 6f;
                s.knockbackResist = Math.min(1f, s.knockbackResist + 0.1f);
            }
            case REGEN -> s.regen = true;
            case SELF_REPAIR -> s.selfRepair = true;
        }
    }

    /** True if the server has turned this module off (disabledModules). */
    public boolean isDisabled() {
        return com.xtrip.golemgarage.Config.listed(com.xtrip.golemgarage.Config.DISABLED_MODULES, id);
    }
}
