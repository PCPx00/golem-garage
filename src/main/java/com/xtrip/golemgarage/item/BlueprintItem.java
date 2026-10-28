package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.golem.GolemMaterial;
import com.xtrip.golemgarage.golem.GolemMaterials;
import com.xtrip.golemgarage.golem.GolemRule;
import com.xtrip.golemgarage.golem.PartShape;
import com.xtrip.golemgarage.golem.PartShapes;
import com.xtrip.golemgarage.golem.PartSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Saves a golem's build: each part's shape and material, plus its rules.
 * Use an empty blueprint on a golem to record it. Use a written one to rebuild:
 * matching parts are taken from your inventory and the rules are copied.
 */
public class BlueprintItem extends Item {
    public BlueprintItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    /** One planned part: shape and material ids. */
    public record Entry(PartShape shape, String material) {
    }

    public static CompoundTag data(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    public static boolean isEmpty(ItemStack stack) {
        return !data(stack).contains("Plan");
    }

    public static Entry entry(ItemStack stack, PartSlot slot) {
        CompoundTag plan = data(stack).getCompound("Plan");
        if (!plan.contains(slot.id())) {
            return null;
        }
        CompoundTag e = plan.getCompound(slot.id());
        PartShape shape = PartShapes.get(e.getString("Shape"));
        return shape == null ? null : new Entry(shape, e.getString("Material"));
    }

    public static List<GolemRule> rules(ItemStack stack) {
        return GolemRule.fromTag(data(stack));
    }

    public static void write(ItemStack stack, java.util.Map<PartSlot, com.xtrip.golemgarage.golem.PartInstance> parts,
                             List<GolemRule> rules) {
        CompoundTag plan = new CompoundTag();
        parts.forEach((slot, p) -> {
            CompoundTag e = new CompoundTag();
            e.putString("Shape", p.shape().id());
            e.putString("Material", p.materialId());
            plan.put(slot.id(), e);
        });
        CompoundTag tag = GolemRule.toTag(rules);
        tag.put("Plan", plan);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (isEmpty(stack)) {
            tooltip.add(Component.translatable("tooltip.golemgarage.bp_empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (PartSlot slot : PartSlot.values()) {
            Entry e = entry(stack, slot);
            if (e != null) {
                GolemMaterial m = GolemMaterials.get(e.material());
                Component mat = m == null ? Component.literal(e.material()) : m.displayName();
                tooltip.add(Component.literal("- ").append(Component.translatable("item.golemgarage.part_format", mat, e.shape().displayName()))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        int ruleCount = rules(stack).size();
        tooltip.add(Component.translatable("tooltip.golemgarage.rules", ruleCount).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.golemgarage.bp_use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
