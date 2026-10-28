package com.xtrip.golemgarage;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Golem Garage settings. Server settings live in each world's serverconfig/golemgarage-server.toml and are
 * synced to players; client settings are in config/golemgarage-client.toml.
 */
public final class Config {
    // ------------------------------------------------------------------ server
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.IntValue REPAIR_SECONDS;
    public static final ModConfigSpec.IntValue REPAIR_COST_PER_PART;
    public static final ModConfigSpec.DoubleValue WEAR_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue WEATHER_WEAR;
    public static final ModConfigSpec.BooleanValue COPPER_AGING;
    public static final ModConfigSpec.DoubleValue POWER_DRAIN_MULTIPLIER;
    public static final ModConfigSpec.IntValue MAX_GOLEMS_PER_PLAYER;
    public static final ModConfigSpec.BooleanValue OTHERS_CAN_PICK_UP;
    public static final ModConfigSpec.BooleanValue OTHERS_CAN_EDIT;
    public static final ModConfigSpec.BooleanValue GIVE_MANUAL;
    public static final ModConfigSpec.BooleanValue GOLEMS_HARVEST_NEED_GRIEFING;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("The Garage").push("garage");
        REPAIR_SECONDS = b.comment("Seconds a full Garage repair takes, before set bonuses and Steel Emitters.")
                .defineInRange("repairSeconds", 120, 5, 3600);
        REPAIR_COST_PER_PART = b.comment("Units of a part's material the Garage charges per damaged part. 0 makes repairs free.")
                .defineInRange("costPerPart", 1, 0, 64);
        b.pop();

        b.comment("Wear and power").push("wear");
        WEAR_MULTIPLIER = b.comment("Multiplies all part wear. 0 turns wear off, 2 doubles it.")
                .defineInRange("wearMultiplier", 1.0, 0.0, 10.0);
        WEATHER_WEAR = b.comment("Rain and desert sand wear down unsealed parts.")
                .define("weatherWear", true);
        COPPER_AGING = b.comment("Copper parts slowly oxidize.")
                .define("copperAging", true);
        POWER_DRAIN_MULTIPLIER = b.comment("Multiplies how fast golems use power. 0 means they never run out.")
                .defineInRange("powerDrainMultiplier", 1.0, 0.0, 10.0);
        b.pop();

        b.comment("Golems and players").push("golems");
        MAX_GOLEMS_PER_PLAYER = b.comment("Most golems one player can have placed in loaded areas at once. 0 = no limit.")
                .defineInRange("maxGolemsPerPlayer", 0, 0, 1000);
        OTHERS_CAN_PICK_UP = b.comment("Other players can pick up golems and cubs they don't own.")
                .define("othersCanPickUp", false);
        OTHERS_CAN_EDIT = b.comment("Other players can open, edit and link golems they don't own.")
                .define("othersCanEdit", false);
        GOLEMS_HARVEST_NEED_GRIEFING = b.comment("Golems only harvest crops when the mobGriefing game rule is on.")
                .define("harvestNeedsMobGriefing", false);
        b.pop();

        b.comment("Getting started").push("general");
        GIVE_MANUAL = b.comment("Give every player a Golem Engineer's Manual the first time they join.")
                .define("giveManualOnJoin", true);
        b.pop();
        SERVER_SPEC = b.build();
    }

    // ------------------------------------------------------------------ client
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_GARAGE_PLAN;
    public static final ModConfigSpec.BooleanValue SHOW_STATUS_ICONS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SHOW_GARAGE_PLAN = b.comment("Show the see-through Garage build plan while holding a Garage Controller.")
                .define("showGaragePlan", true);
        SHOW_STATUS_ICONS = b.comment("Show little icons over golems that are stuck, out of power or have a broken part.")
                .define("showStatusIcons", true);
        CLIENT_SPEC = b.build();
    }

    private Config() {
    }
}
