package com.xtrip.golemgarage.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.golem.GolemMaterial;
import com.xtrip.golemgarage.golem.GolemMaterials;
import com.xtrip.golemgarage.golem.GolemTrait;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Golem materials added by datapacks: data/&lt;namespace&gt;/golem_material/&lt;id&gt;.json.
 * <pre>
 * {
 *   "name": "Mythril",              // shown in game (or add material.golemgarage.&lt;id&gt; to a language file)
 *   "item_tag": "c:ingots/mythril", // the ingredient for its parts and Garage repairs
 *   "tier": 4,                      // 0 (stone) .. 9; drives health, durability, power and damage
 *   "base": "iron",                 // a built-in material whose textures it borrows
 *   "color": "#7fd3ff",             // tint laid over those textures
 *   "durability": 1.0, "speed": 1.0, "power": 1.0, "work": 1.0,   // optional multipliers
 *   "traits": ["luminous"]          // optional: oxidizes, conductive, hardened, fireproof, heavy, smite,
 *                                   //           luminous, ender, thorns, featherweight
 * }
 * </pre>
 * Parts are crafted with the usual shape recipes using the tagged item. The list is sent to every player.
 */
public class DatapackMaterials extends SimpleJsonResourceReloadListener {
    public static final String FOLDER = "golem_material";

    public DatapackMaterials() {
        super(new Gson(), FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, GolemMaterials.Extra> out = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            String id = e.getKey().getPath().toLowerCase(Locale.ROOT).replace('/', '_');
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                if (GolemMaterials.isBuiltIn(id)) {
                    GolemGarage.LOGGER.warn("Golem Garage: datapack material {} has the same id as a built-in one; skipped", e.getKey());
                    continue;
                }
                out.put(id, read(id, o));
            } catch (RuntimeException ex) {
                GolemGarage.LOGGER.error("Golem Garage: couldn't read golem material {}: {}", e.getKey(), ex.getMessage());
            }
        }
        GolemMaterials.setExtras(out);
        if (!out.isEmpty()) {
            GolemGarage.LOGGER.info("Golem Garage: loaded {} datapack golem material(s): {}", out.size(), out.keySet());
        }
    }

    private static GolemMaterials.Extra read(String id, JsonObject o) {
        String tag = GsonHelper.getAsString(o, "item_tag");
        int tier = Math.max(0, Math.min(9, GsonHelper.getAsInt(o, "tier", 2)));
        String base = GsonHelper.getAsString(o, "base", "iron");
        if (!GolemMaterials.isBuiltIn(base)) {
            base = "iron";
        }
        String hex = GsonHelper.getAsString(o, "color", "#ffffff").replace("#", "");
        int color = (int) Long.parseLong(hex, 16) & 0xFFFFFF;
        Set<GolemTrait> traits = EnumSet.noneOf(GolemTrait.class);
        if (o.has("traits")) {
            for (JsonElement t : o.getAsJsonArray("traits")) {
                traits.add(GolemTrait.valueOf(t.getAsString().toUpperCase(Locale.ROOT)));
            }
        }
        GolemMaterial m = new GolemMaterial(id, tier, tag, color,
                GsonHelper.getAsFloat(o, "durability", 1f), GsonHelper.getAsFloat(o, "speed", 1f),
                GsonHelper.getAsFloat(o, "power", 1f), GsonHelper.getAsFloat(o, "work", 1f),
                traits, null, true);
        return new GolemMaterials.Extra(m, base, color, GsonHelper.getAsString(o, "name", pretty(id)));
    }

    private static String pretty(String id) {
        StringBuilder b = new StringBuilder();
        for (String w : id.split("_")) {
            if (!w.isEmpty()) {
                b.append(b.isEmpty() ? "" : " ").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
        }
        return b.toString();
    }

    // ------------------------------------------------------------------ syncing to players
    public static CompoundTag write(Map<String, GolemMaterials.Extra> extras) {
        ListTag list = new ListTag();
        for (GolemMaterials.Extra x : extras.values()) {
            GolemMaterial m = x.material();
            CompoundTag t = new CompoundTag();
            t.putString("Id", m.id());
            t.putString("Tag", m.tag());
            t.putInt("Tier", m.tier());
            t.putString("Base", x.base());
            t.putInt("Color", x.tint());
            t.putString("Name", x.name());
            t.putFloat("Dur", m.durMultRaw());
            t.putFloat("Speed", m.speedMultRaw());
            t.putFloat("Power", m.powerMultRaw());
            t.putFloat("Work", m.workMultRaw());
            ListTag tr = new ListTag();
            for (GolemTrait trait : m.traits()) {
                tr.add(StringTag.valueOf(trait.name()));
            }
            t.put("Traits", tr);
            list.add(t);
        }
        CompoundTag root = new CompoundTag();
        root.put("Materials", list);
        return root;
    }

    public static Map<String, GolemMaterials.Extra> read(CompoundTag root) {
        Map<String, GolemMaterials.Extra> out = new LinkedHashMap<>();
        for (Tag raw : root.getList("Materials", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            Set<GolemTrait> traits = EnumSet.noneOf(GolemTrait.class);
            for (Tag tr : t.getList("Traits", Tag.TAG_STRING)) {
                traits.add(GolemTrait.valueOf(tr.getAsString()));
            }
            String id = t.getString("Id");
            GolemMaterial m = new GolemMaterial(id, t.getInt("Tier"), t.getString("Tag"), t.getInt("Color"),
                    t.getFloat("Dur"), t.getFloat("Speed"), t.getFloat("Power"), t.getFloat("Work"), traits, null, true);
            out.put(id, new GolemMaterials.Extra(m, t.getString("Base"), t.getInt("Color"), t.getString("Name")));
        }
        return out;
    }
}
