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
        GolemMaterials.Extra x = GolemMaterials.extra(id);
        return x == null ? Component.translatable("material.golemgarage." + id)
                : Component.translatableWithFallback("material.golemgarage." + id, x.name());
    }

    // Unscaled multipliers, for syncing datapack materials.
    public float durMultRaw() {
        return durMult;
    }

    public float speedMultRaw() {
        return speedMult;
    }

    public float powerMultRaw() {
        return powerMult;
    }

    public float workMultRaw() {
        return workMult;
    }

    // Server settings can scale each material (materialStats); these accessors apply that.
    @Override
    public float durMult() {
        return durMult * com.xtrip.golemgarage.Config.materialStat(id, "durability");
    }

    @Override
    public float speedMult() {
        return speedMult * com.xtrip.golemgarage.Config.materialStat(id, "speed");
    }

    @Override
    public float powerMult() {
        return powerMult * com.xtrip.golemgarage.Config.materialStat(id, "power");
    }

    @Override
    public float workMult() {
        return workMult * com.xtrip.golemgarage.Config.materialStat(id, "work");
    }

    /** True if the server has blocked this material (blockedMaterials). */
    public boolean isBlocked() {
        return com.xtrip.golemgarage.Config.listed(com.xtrip.golemgarage.Config.BLOCKED_MATERIALS, id);
    }

    public boolean has(GolemTrait trait) {
        return traits.contains(trait);
    }

    /** Health multiplier for parts of this material. */
    public float healthMult() {
        return (1.0f + 0.3f * tier) * com.xtrip.golemgarage.Config.materialStat(id, "health");
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
