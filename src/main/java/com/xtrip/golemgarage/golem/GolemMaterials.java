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

    /** The material with this id, or null. */
    public static GolemMaterial get(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** The material with this id, or iron if it is unknown (for example a removed optional mod). */
    public static GolemMaterial getOrIron(String id) {
        GolemMaterial m = get(id);
        return m == null ? IRON : m;
    }

    /** 1-based index used by item model overrides; 0 if unknown. */
    public static int craftableIndex(String id) {
        for (int i = 0; i < CRAFTABLE.size(); i++) {
            if (CRAFTABLE.get(i).id().equals(id)) {
                return i + 1;
            }
        }
        return 0;
    }

    /** 1-based index over all materials; 0 if unknown. */
    public static int allIndex(String id) {
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
