package com.xtrip.golemgarage.golem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The material list. The data lives in GeneratedMaterials (made by tools/golemgen). */
public final class GolemMaterials {
    public static final List<GolemMaterial> ALL = GeneratedMaterials.ALL;
    public static final List<GolemMaterial> CRAFTABLE = ALL.stream().filter(GolemMaterial::craftable).toList();
    private static final Map<String, GolemMaterial> BY_ID = new LinkedHashMap<>();

    static {
        for (GolemMaterial m : ALL) {
            BY_ID.put(m.id(), m);
        }
    }

    public static final GolemMaterial IRON = BY_ID.get("iron");
    public static final GolemMaterial FRAME = BY_ID.get("frame");

    /** Copper oxidation stages, used for textures. Index = oxidation level 0..3. */
    public static final String[] COPPER_STAGES = {"copper", "exposed_copper", "weathered_copper", "oxidized_copper"};

    /** A material added by a datapack: its stats, the built-in material whose textures it borrows, and a tint. */
    public record Extra(GolemMaterial material, String base, int tint, String name) {
    }

    private static volatile Map<String, Extra> extras = Map.of();

    public static void setExtras(Map<String, Extra> loaded) {
        extras = Map.copyOf(loaded);
    }

    public static Map<String, Extra> extras() {
        return extras;
    }

    @org.jetbrains.annotations.Nullable
    public static Extra extra(String id) {
        return id == null ? null : extras.get(id);
    }

    public static boolean isBuiltIn(String id) {
        return BY_ID.containsKey(id);
    }

    /** The material with this id (built-in or from a datapack), or null. */
    public static GolemMaterial get(String id) {
        if (id == null) {
            return null;
        }
        GolemMaterial m = BY_ID.get(id);
        if (m != null) {
            return m;
        }
        Extra x = extras.get(id);
        return x == null ? null : x.material();
    }

    /** The built-in material whose textures and icons this one uses (itself for built-ins). */
    public static String lookOf(String id) {
        Extra x = extras.get(id);
        return x == null ? id : x.base();
    }

    /** ARGB tint for a material's textures: white for built-ins. */
    public static int tintOf(String id) {
        Extra x = extras.get(id);
        return x == null ? 0xFFFFFFFF : 0xFF000000 | x.tint();
    }

    /** The material with this id, or iron if it is unknown (for example a removed optional mod). */
    public static GolemMaterial getOrIron(String id) {
        GolemMaterial m = get(id);
        return m == null ? IRON : m;
    }

    /** 1-based index used by item model overrides; 0 if unknown. */
    public static int craftableIndex(String id) {
        id = lookOf(id);
        for (int i = 0; i < CRAFTABLE.size(); i++) {
            if (CRAFTABLE.get(i).id().equals(id)) {
                return i + 1;
            }
        }
        return 0;
    }

    /** 1-based index over all materials; 0 if unknown. */
    public static int allIndex(String id) {
        id = lookOf(id);
        for (int i = 0; i < ALL.size(); i++) {
            if (ALL.get(i).id().equals(id)) {
                return i + 1;
            }
        }
        return 0;
    }

    private GolemMaterials() {
    }
}
