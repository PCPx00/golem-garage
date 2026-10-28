package com.xtrip.golemgarage.golem;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;

/** Reading and writing a golem's parts in saved data (entity saves, golem items, blueprints). */
public final class GolemData {

    public static Map<PartSlot, PartInstance> readParts(CompoundTag golemTag) {
        Map<PartSlot, PartInstance> parts = new EnumMap<>(PartSlot.class);
        CompoundTag partTag = golemTag.getCompound("Parts");
        for (PartSlot slot : PartSlot.values()) {
            if (partTag.contains(slot.id(), CompoundTag.TAG_COMPOUND)) {
                PartInstance p = PartInstance.load(partTag.getCompound(slot.id()));
                if (p != null && p.shape().slot() == slot) {
                    parts.put(slot, p);
                }
            }
        }
        return parts;
    }

    public static CompoundTag writeParts(Map<PartSlot, PartInstance> parts) {
        CompoundTag partTag = new CompoundTag();
        for (Map.Entry<PartSlot, PartInstance> e : parts.entrySet()) {
            partTag.put(e.getKey().id(), e.getValue().save());
        }
        return partTag;
    }

    /** A full golem save tag with every slot made from one material. Used for creative presets. */
    public static CompoundTag preset(GolemMaterial mat, String head, String torso, String arms, String legs, String core) {
        Map<PartSlot, PartInstance> parts = new EnumMap<>(PartSlot.class);
        put(parts, head, mat);
        put(parts, torso, mat);
        put(parts, arms, mat);
        put(parts, legs, mat);
        put(parts, core, mat);
        CompoundTag tag = new CompoundTag();
        tag.put("Parts", writeParts(parts));
        return tag;
    }

    private static void put(Map<PartSlot, PartInstance> parts, String shapeId, GolemMaterial mat) {
        PartShape s = PartShapes.get(shapeId);
        if (s != null) {
            parts.put(s.slot(), new PartInstance(s, mat));
        }
    }

    private GolemData() {
    }
}
