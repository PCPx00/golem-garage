package com.xtrip.golemgarage.golem;

import net.minecraft.network.chat.Component;

import java.util.Set;

/**
 * A part shape: what the part is (Sentry Head, Wheel Legs, ...). Combined with a material
 * it makes a part. Only the stats that matter for its slot are non-zero.
 */
public record PartShape(String id, PartSlot slot, float health, float vision, int carry,
                        float attack, float work, float speed, float step, float knockbackResist,
                        int capacity, float slotDurability, Set<ShapeAbility> abilities, int glowColor) {

    public boolean has(ShapeAbility ability) {
        return abilities.contains(ability);
    }

    public Component displayName() {
        return Component.translatable(displayKey());
    }

    public String displayKey() {
        return "item.golemgarage." + id;
    }

    /** Maximum durability of this shape made from the material. Matches the generator's recipes. */
    public int maxDurability(GolemMaterial material) {
        double v = (60.0 + 40.0 * material.tier()) * material.durMult() * slotDurability;
        return (int) Math.floor(v + 0.5);
    }
}
