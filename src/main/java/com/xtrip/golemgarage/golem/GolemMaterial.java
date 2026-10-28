package com.xtrip.golemgarage.golem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * A part material. Matches ingots by item tag, so any mod that adds the ingot works.
 * Stats scale with tier; the multipliers tune individual materials.
 */
public record GolemMaterial(String id, int tier, String tag, int color,
                            float durMult, float speedMult, float powerMult, float workMult,
                            Set<GolemTrait> traits, String family, boolean craftable) {

    public Component displayName() {
        return Component.translatable("material.golemgarage." + id);
    }

    public boolean has(GolemTrait trait) {
        return traits.contains(trait);
    }

    /** Health multiplier for parts of this material. */
    public float healthMult() {
        return 1.0f + 0.3f * tier;
    }

    /** Extra melee damage from arms of this material. */
    public float attackBonus() {
        return 1.5f * tier;
    }

    /** Armor points one part of this material adds. */
    public float armorPerPart() {
        return 0.8f * tier;
    }

    /** Power capacity multiplier for a core of this material. */
    public float capacityMult() {
        return (1.0f + 0.25f * tier) * powerMult;
    }

    public TagKey<Item> tagKey() {
        return tag == null ? null : TagKey.create(Registries.ITEM, ResourceLocation.parse(tag));
    }

    /** True if the item is this material's ingredient (an ingot, gem or cobblestone). */
    public boolean matches(ItemStack stack) {
        TagKey<Item> key = tagKey();
        return key != null && stack.is(key);
    }

    /** True if some loaded mod provides this material's ingredient. */
    public boolean isAvailable() {
        TagKey<Item> key = tagKey();
        return craftable && key != null
                && BuiltInRegistries.ITEM.getTag(key).map(t -> t.size() > 0).orElse(false);
    }
}
