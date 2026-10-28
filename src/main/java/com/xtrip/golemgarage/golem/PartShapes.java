package com.xtrip.golemgarage.golem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The shape list. The data lives in GeneratedShapes (made by tools/golemgen). */
public final class PartShapes {
    public static final List<PartShape> ALL = GeneratedShapes.ALL;
    private static final Map<String, PartShape> BY_ID = new LinkedHashMap<>();

    static {
        for (PartShape s : ALL) {
            BY_ID.put(s.id(), s);
        }
    }

    public static PartShape get(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static List<PartShape> forSlot(PartSlot slot) {
        return ALL.stream().filter(s -> s.slot() == slot).toList();
    }

    private PartShapes() {
    }
}
