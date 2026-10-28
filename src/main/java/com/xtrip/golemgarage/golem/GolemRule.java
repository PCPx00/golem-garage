package com.xtrip.golemgarage.golem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * One rule: WHEN trigger [param] DO action [actionParam], optionally UNLESS another trigger.
 * Rules are read top to bottom; the first rule that applies runs.
 */
public record GolemRule(Trigger trigger, int param, Action action, int actionParam, Trigger unless, int unlessParam) {

    public static final int MAX_RULES = 8;

    public GolemRule(Trigger trigger, int param, Action action) {
        this(trigger, param, action, action.defaultParam, Trigger.NONE, 0);
    }

    public enum Trigger {
        ALWAYS("always"),
        HEALTH_BELOW("health below %", 10, 90, 10, 50),
        POWER_BELOW("power below %", 10, 90, 10, 20),
        PART_WORN("a part below %", 10, 90, 10, 50),
        PART_BROKEN("a part is broken"),
        OWNER_WITHIN("owner within", 2, 32, 2, 8),
        OWNER_BEYOND("owner beyond", 4, 48, 4, 16),
        OWNER_ATTACKED("owner attacked"),
        HOSTILE_WITHIN("hostile within", 2, 24, 2, 8),
        AWAY_FROM_POST("away from post", 1, 16, 1, 2),
        CROP_READY("crop ready within", 2, 16, 2, 8),
        CHEST_A_HAS("chest A has", 1, 64, 1, 1),
        CHEST_A_BELOW("chest A below", 1, 64, 4, 16),
        CHEST_B_FULL("chest B full"),
        INVENTORY_FULL("inventory full"),
        INVENTORY_EMPTY("inventory empty"),
        DAY("day"),
        SUNSET("sunset"),
        NIGHT("night"),
        RAINING("raining"),
        THUNDER("thunderstorm"),
        REDSTONE_ON("redstone on"),
        REDSTONE_OFF("redstone off"),
        OUTSIDE_AREA("outside its area"),
        NONE("-");

        public final String label;
        public final boolean hasParam;
        public final int min;
        public final int max;
        public final int step;
        public final int defaultParam;

        Trigger(String label) {
            this(label, 0, 0, 0, 0, false);
        }

        Trigger(String label, int min, int max, int step, int def) {
            this(label, min, max, step, def, true);
        }

        Trigger(String label, int min, int max, int step, int def, boolean hasParam) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.defaultParam = def;
            this.hasParam = hasParam;
        }
    }

    public enum Action {
        FOLLOW_OWNER("follow owner"),
        GO_TO_POST("go to post"),
        WAIT("wait s", 1, 60, 1, 5),
        STROLL("stroll"),
        ATTACK_HOSTILE("attack hostile"),
        DEFEND_OWNER("defend owner"),
        TAKE_FROM_A("take from chest A"),
        DEPOSIT_TO_B("deposit to chest B"),
        TAKE_FROM_NEAREST("take from chest"),
        DEPOSIT_TO_NEAREST("deposit to chest"),
        PICK_UP_ITEMS("pick up items"),
        HARVEST_CROPS("harvest crops"),
        RECHARGE("recharge"),
        REPAIR_SELF("repair self"),
        GO_TO_GARAGE("go to Garage"),
        EMIT_SIGNAL("emit redstone"),
        GIVE_TO_GOLEM("give items to a golem"),
        PATROL("patrol its area");

        public final String label;
        public final boolean hasParam;
        public final int min;
        public final int max;
        public final int step;
        public final int defaultParam;

        Action(String label) {
            this(label, 0, 0, 0, 0, false);
        }

        Action(String label, int min, int max, int step, int def) {
            this(label, min, max, step, def, true);
        }

        Action(String label, int min, int max, int step, int def, boolean hasParam) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.defaultParam = def;
            this.hasParam = hasParam;
        }
    }

    /** A short sentence for the status panel. */
    public String describe() {
        StringBuilder sb = new StringBuilder("WHEN ").append(trigger.label);
        if (trigger.hasParam) {
            sb.append(' ').append(param);
        }
        sb.append(" DO ").append(action.label);
        if (action.hasParam) {
            sb.append(' ').append(actionParam);
        }
        if (unless != Trigger.NONE) {
            sb.append(" UNLESS ").append(unless.label);
            if (unless.hasParam) {
                sb.append(' ').append(unlessParam);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ presets
    public static List<GolemRule> followerPreset() {
        List<GolemRule> r = new ArrayList<>();
        r.add(new GolemRule(Trigger.OWNER_ATTACKED, 0, Action.DEFEND_OWNER));
        r.add(new GolemRule(Trigger.ALWAYS, 0, Action.FOLLOW_OWNER));
        return r;
    }

    public static List<GolemRule> guardPreset() {
        List<GolemRule> r = new ArrayList<>();
        r.add(new GolemRule(Trigger.HOSTILE_WITHIN, 10, Action.ATTACK_HOSTILE));
        r.add(new GolemRule(Trigger.OWNER_ATTACKED, 0, Action.DEFEND_OWNER));
        r.add(new GolemRule(Trigger.AWAY_FROM_POST, 2, Action.GO_TO_POST));
        r.add(new GolemRule(Trigger.ALWAYS, 0, Action.WAIT));
        return r;
    }

    public static List<GolemRule> haulerPreset() {
        List<GolemRule> r = new ArrayList<>();
        r.add(new GolemRule(Trigger.POWER_BELOW, 20, Action.RECHARGE));
        r.add(new GolemRule(Trigger.INVENTORY_FULL, 0, Action.DEPOSIT_TO_B));
        r.add(new GolemRule(Trigger.CHEST_A_HAS, 1, Action.TAKE_FROM_A, 0, Trigger.INVENTORY_FULL, 0));
        r.add(new GolemRule(Trigger.INVENTORY_EMPTY, 0, Action.GO_TO_POST, 0, Trigger.NONE, 0));
        r.add(new GolemRule(Trigger.ALWAYS, 0, Action.DEPOSIT_TO_B));
        return r;
    }

    public static List<GolemRule> harvesterPreset() {
        List<GolemRule> r = new ArrayList<>();
        r.add(new GolemRule(Trigger.POWER_BELOW, 20, Action.RECHARGE));
        r.add(new GolemRule(Trigger.INVENTORY_FULL, 0, Action.DEPOSIT_TO_NEAREST));
        r.add(new GolemRule(Trigger.CROP_READY, 10, Action.HARVEST_CROPS));
        r.add(new GolemRule(Trigger.ALWAYS, 0, Action.PICK_UP_ITEMS));
        return r;
    }

    public static List<GolemRule> medicPreset() {
        List<GolemRule> r = new ArrayList<>();
        r.add(new GolemRule(Trigger.PART_BROKEN, 0, Action.GO_TO_GARAGE));
        r.add(new GolemRule(Trigger.HEALTH_BELOW, 50, Action.GO_TO_GARAGE));
        r.add(new GolemRule(Trigger.PART_WORN, 40, Action.REPAIR_SELF));
        r.add(new GolemRule(Trigger.ALWAYS, 0, Action.FOLLOW_OWNER));
        return r;
    }

    public static List<GolemRule> nightGuardPreset() {
        List<GolemRule> r = new ArrayList<>();
        r.add(new GolemRule(Trigger.POWER_BELOW, 20, Action.RECHARGE));
        r.add(new GolemRule(Trigger.HOSTILE_WITHIN, 8, Action.ATTACK_HOSTILE));
        r.add(new GolemRule(Trigger.NIGHT, 0, Action.GO_TO_POST));
        r.add(new GolemRule(Trigger.ALWAYS, 0, Action.FOLLOW_OWNER));
        return r;
    }

    // ------------------------------------------------------------------ save / load
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("T", trigger.name());
        tag.putInt("P", param);
        tag.putString("A", action.name());
        tag.putInt("AP", actionParam);
        if (unless != Trigger.NONE) {
            tag.putString("U", unless.name());
            tag.putInt("UP", unlessParam);
        }
        return tag;
    }

    /** Returns the rule, or null if the saved data names something that no longer exists. */
    public static GolemRule load(CompoundTag tag) {
        try {
            Trigger t = Trigger.valueOf(tag.getString("T"));
            String a = tag.getString("A");
            // Names used by earlier versions.
            if (a.equals("PULL_FROM_CHEST")) {
                a = "TAKE_FROM_NEAREST";
            } else if (a.equals("DEPOSIT_TO_CHEST")) {
                a = "DEPOSIT_TO_NEAREST";
            }
            Action action = Action.valueOf(a);
            int ap = tag.contains("AP") ? tag.getInt("AP") : action.defaultParam;
            Trigger u = tag.contains("U") ? Trigger.valueOf(tag.getString("U")) : Trigger.NONE;
            return new GolemRule(t, tag.getInt("P"), action, ap, u, tag.getInt("UP"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static CompoundTag toTag(List<GolemRule> rules) {
        ListTag list = new ListTag();
        for (GolemRule rule : rules) {
            list.add(rule.save());
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Rules", list);
        return tag;
    }

    public static List<GolemRule> fromTag(CompoundTag tag) {
        List<GolemRule> rules = new ArrayList<>();
        if (tag.contains("Rules", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Rules", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && rules.size() < MAX_RULES; i++) {
                GolemRule rule = load(list.getCompound(i));
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }
        return rules;
    }
}
