package com.xtrip.golemgarage;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Golem Garage settings. Server settings live in each world's serverconfig/golemgarage-server.toml and are
 * synced to players; client settings are in config/golemgarage-client.toml.
 */
public final class Config {
    /** Whether golems may fight players. */
    public enum PlayerDamage { NEVER, PVP, ALWAYS }

    // ------------------------------------------------------------------ server
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.IntValue REPAIR_SECONDS;
    public static final ModConfigSpec.IntValue REPAIR_COST_PER_PART;
    public static final ModConfigSpec.DoubleValue WEAR_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue WEATHER_WEAR;
    public static final ModConfigSpec.BooleanValue COPPER_AGING;
    public static final ModConfigSpec.DoubleValue POWER_DRAIN_MULTIPLIER;
    public static final ModConfigSpec.IntValue MAX_GOLEMS_PER_PLAYER;
    public static final ModConfigSpec.IntValue MAX_GOLEMS_PER_CHUNK;
    public static final ModConfigSpec.IntValue WORK_RANGE_FROM_OWNER;
    public static final ModConfigSpec.BooleanValue OTHERS_CAN_PICK_UP;
    public static final ModConfigSpec.BooleanValue OTHERS_CAN_EDIT;
    public static final ModConfigSpec.EnumValue<PlayerDamage> PLAYER_DAMAGE;
    public static final ModConfigSpec.BooleanValue KILLS_COUNT_AS_OWNER;
    public static final ModConfigSpec.BooleanValue GIVE_MANUAL;
    public static final ModConfigSpec.BooleanValue GOLEMS_HARVEST_NEED_GRIEFING;
    public static final ModConfigSpec.IntValue ITEM_DESPAWN_MINUTES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_ACTIONS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_MODULES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_SHAPES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_MATERIALS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MATERIAL_STATS;
    public static final ModConfigSpec.IntValue CUB_MAGNET_RANGE;
    public static final ModConfigSpec.IntValue MAX_CUBS_PER_PLAYER;
    public static final ModConfigSpec.BooleanValue PLAYER_COMMANDS;
    public static final ModConfigSpec.IntValue RECALL_COOLDOWN_SECONDS;

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
        MAX_GOLEMS_PER_PLAYER = b.comment("Most golems one player can have out at once. 0 = no limit.")
                .defineInRange("maxGolemsPerPlayer", 0, 0, 1000);
        MAX_GOLEMS_PER_CHUNK = b.comment("Most golems that can be placed in one chunk (16x16 area). 0 = no limit.")
                .defineInRange("maxGolemsPerChunk", 0, 0, 1000);
        WORK_RANGE_FROM_OWNER = b.comment("Golems pause their jobs while their owner is offline or further away than this",
                        "many blocks (or in another dimension). 0 = they always work.")
                .defineInRange("workRangeFromOwner", 0, 0, 100000);
        OTHERS_CAN_PICK_UP = b.comment("Other players can pick up golems and cubs they don't own.")
                .define("othersCanPickUp", false);
        OTHERS_CAN_EDIT = b.comment("Other players can open, edit and link golems they don't own.")
                .define("othersCanEdit", false);
        PLAYER_DAMAGE = b.comment("Whether golems fight players: NEVER, PVP (only other players, and only when PvP is on)",
                        "or ALWAYS (any player but their owner).")
                .defineEnum("playerDamage", PlayerDamage.PVP);
        KILLS_COUNT_AS_OWNER = b.comment("Monsters killed by a golem drop experience and player-only loot, as if its owner",
                        "had killed them (the owner must be online and in the same dimension).")
                .define("killsCountAsOwner", false);
        GOLEMS_HARVEST_NEED_GRIEFING = b.comment("Golems only harvest crops when the mobGriefing game rule is on.")
                .define("harvestNeedsMobGriefing", false);
        ITEM_DESPAWN_MINUTES = b.comment("Minutes a dropped golem or cub item lies on the ground before it despawns.",
                        "0 = it never despawns. (Other items despawn after 5 minutes.)")
                .defineInRange("itemDespawnMinutes", 0, 0, 1440);
        b.pop();

        b.comment("Turning features off").push("features");
        DISABLED_ACTIONS = b.comment("Rule actions golems can't use, by name, for example [\"harvest_crops\", \"pick_up_items\"].",
                        "Names: follow_owner, go_to_post, wait, stroll, attack_hostile, defend_owner, take_from_a,",
                        "deposit_to_b, take_from_nearest, deposit_to_nearest, pick_up_items, harvest_crops, recharge,",
                        "repair_self, go_to_garage, emit_signal, give_to_golem, patrol, go_home.")
                .defineListAllowEmpty("disabledActions", List.of(), () -> "", o -> o instanceof String);
        DISABLED_MODULES = b.comment("Upgrade modules that can't be installed: magnet, lamp, speed, storage, plating, regen, self_repair.")
                .defineListAllowEmpty("disabledModules", List.of(), () -> "", o -> o instanceof String);
        DISABLED_SHAPES = b.comment("Part shapes that can't be fitted to golems, by id, for example [\"scythe_arms\"].")
                .defineListAllowEmpty("disabledShapes", List.of(), () -> "", o -> o instanceof String);
        b.pop();

        b.comment("Materials").push("materials");
        BLOCKED_MATERIALS = b.comment("Materials whose parts can't be fitted to golems, by id, for example [\"netherite\"].")
                .defineListAllowEmpty("blockedMaterials", List.of(), () -> "", o -> o instanceof String);
        MATERIAL_STATS = b.comment("Per-material multipliers, one entry per material:",
                        "\"material;health=1.2;durability=1.5;speed=1.0;power=1.0;work=1.0;damage=1.0\".",
                        "Leave out any stat to keep it as it is. Example: [\"diamond;durability=2\", \"gold;speed=1.2\"]")
                .defineListAllowEmpty("materialStats", List.of(), () -> "", o -> o instanceof String);
        b.pop();

        b.comment("Cubs").push("cubs");
        CUB_MAGNET_RANGE = b.comment("How many blocks away a cub's item magnet picks up items. 0 turns the magnet off.")
                .defineInRange("magnetRange", 3, 0, 16);
        MAX_CUBS_PER_PLAYER = b.comment("Most cubs one player can have out at once. 0 = no limit.")
                .defineInRange("maxCubsPerPlayer", 0, 0, 1000);
        b.pop();

        b.comment("Commands and getting started").push("general");
        GIVE_MANUAL = b.comment("Give every player a Golem Engineer's Manual the first time they join.")
                .define("giveManualOnJoin", true);
        PLAYER_COMMANDS = b.comment("Players can use /golemgarage list, find, recall, rename, stop and resume on their own golems.",
                        "Admin commands always need operator permission.")
                .define("playerCommands", true);
        RECALL_COOLDOWN_SECONDS = b.comment("Seconds a player must wait between /golemgarage recall uses. Operators skip it.")
                .defineInRange("recallCooldownSeconds", 60, 0, 86400);
        b.pop();
        SERVER_SPEC = b.build();
    }

    // ------------------------------------------------------------------ client
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_GARAGE_PLAN;
    public static final ModConfigSpec.BooleanValue SHOW_STATUS_ICONS;
    public static final ModConfigSpec.IntValue GOLEM_VOLUME;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SHOW_GARAGE_PLAN = b.comment("Show the see-through Garage build plan while holding a Garage Controller.")
                .define("showGaragePlan", true);
        SHOW_STATUS_ICONS = b.comment("Show little icons over golems that are stuck, out of power or have a broken part.")
                .define("showStatusIcons", true);
        GOLEM_VOLUME = b.comment("Volume of golem and cub sounds, in percent (on top of the Friendly Creatures slider).")
                .defineInRange("golemVolume", 100, 0, 100);
        CLIENT_SPEC = b.build();
    }

    // ------------------------------------------------------------------ safe reads
    /** Reads a setting, or returns the fallback before the settings file is loaded (main menu, JEI start-up). */
    public static <T> T get(Supplier<T> value, T fallback) {
        try {
            return value.get();
        } catch (IllegalStateException | NullPointerException notLoaded) {
            return fallback;
        }
    }

    public static boolean listed(ModConfigSpec.ConfigValue<List<? extends String>> list, String id) {
        List<? extends String> l = get(list::get, List.of());
        String want = id.toLowerCase(Locale.ROOT);
        for (String s : l) {
            if (s.trim().toLowerCase(Locale.ROOT).equals(want)) {
                return true;
            }
        }
        return false;
    }

    private static List<? extends String> statsSeen;
    private static Map<String, Map<String, Float>> statsParsed = Map.of();

    /** The multiplier for one stat of one material (1 when not set). */
    public static float materialStat(String material, String stat) {
        List<? extends String> raw = get(MATERIAL_STATS::get, List.of());
        if (raw != statsSeen) {
            Map<String, Map<String, Float>> parsed = new HashMap<>();
            for (String entry : raw) {
                String[] bits = entry.split(";");
                if (bits.length < 2) {
                    continue;
                }
                Map<String, Float> m = new HashMap<>();
                for (int i = 1; i < bits.length; i++) {
                    String[] kv = bits[i].split("=");
                    if (kv.length == 2) {
                        try {
                            m.put(kv[0].trim().toLowerCase(Locale.ROOT), Math.max(0f, Float.parseFloat(kv[1].trim())));
                        } catch (NumberFormatException ignored) {
                            GolemGarage.LOGGER.warn("Golem Garage: bad number in materialStats entry \"{}\"", entry);
                        }
                    }
                }
                parsed.put(bits[0].trim().toLowerCase(Locale.ROOT), m);
            }
            statsParsed = parsed;
            statsSeen = raw;
        }
        Map<String, Float> m = statsParsed.get(material);
        return m == null ? 1f : m.getOrDefault(stat, 1f);
    }

    private Config() {
    }
}
