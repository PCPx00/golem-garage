package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.golem.GolemMaterial;
import com.xtrip.golemgarage.golem.GolemMaterials;
import com.xtrip.golemgarage.golem.GolemTrait;
import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.golem.PartShape;
import com.xtrip.golemgarage.golem.ShapeAbility;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A golem part: one item per shape, with the material stored on the stack.
 * Right-click a golem with it to fit it. Repair it in an anvil with its material.
 */
public class PartItem extends Item {
    private final PartShape shape;

    public PartItem(PartShape shape, Properties properties) {
        super(properties.stacksTo(1));
        this.shape = shape;
    }

    public PartShape shape() {
        return shape;
    }

    public static ItemStack create(PartShape shape, GolemMaterial material) {
        return new PartInstance(shape, material).toStack();
    }

    public static String materialId(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MATERIAL.get(), "iron");
    }

    @Override
    public Component getName(ItemStack stack) {
        String id = materialId(stack);
        GolemMaterial mat = GolemMaterials.get(id);
        Component matName = mat == null ? Component.translatable("tooltip.golemgarage.unknown") : mat.displayName();
        return Component.translatable("item.golemgarage.part_format", matName, shape.displayName());
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        GolemMaterial mat = GolemMaterials.get(materialId(stack));
        return mat != null && mat.matches(repair);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PartInstance part = PartInstance.fromStack(stack);
        if (part == null) {
            return;
        }
        GolemMaterial mat = part.material();
        tooltip.add(Component.translatable("item.golemgarage." + shape.id() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.golemgarage.part_slot", Component.translatable("golemgarage.slot." + shape.slot().id()), mat.tier()).withStyle(ChatFormatting.DARK_GRAY));
        if (part.isMissingMaterial()) {
            tooltip.add(Component.translatable("tooltip.golemgarage.missing_mod").withStyle(ChatFormatting.RED));
        }
        for (Component line : statLines(part)) {
            tooltip.add(line);
        }
        for (GolemTrait trait : mat.traits()) {
            tooltip.add(Component.literal("+ ").append(traitText(trait)).withStyle(ChatFormatting.AQUA));
        }
        if (part.oxidation() > 0) {
            tooltip.add(Component.translatable("tooltip.golemgarage.oxidation", Component.translatable("tooltip.golemgarage.ox1".replace("ox1", "ox" + part.oxidation())), 3 * part.oxidation()).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (part.waxed()) {
            tooltip.add(Component.translatable("tooltip.golemgarage.waxed").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("tooltip.golemgarage.durability", part.durability(), part.maxDurability())
                .withStyle(part.isBroken() ? ChatFormatting.RED : ChatFormatting.DARK_GREEN));
    }

    private String slotName() {
        String s = shape.slot().id();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private List<Component> statLines(PartInstance part) {
        GolemMaterial mat = part.material();
        MutableComponent a;
        return switch (shape.slot()) {
            case HEAD -> List.of(
                    stat("sight", Component.translatable("tooltip.golemgarage.blocks", Math.round(shape.vision() + 2 * mat.tier()))),
                    stat("health", Component.literal("+" + Math.round(shape.health() * mat.healthMult()))));
            case TORSO -> List.of(
                    stat("health", Component.literal("+" + Math.round(shape.health() * mat.healthMult()))),
                    stat("carries", Component.translatable("tooltip.golemgarage.stacks", Math.min(36, shape.carry() + (mat.tier() >= 5 ? 9 : 0)))));
            case ARMS -> List.of(
                    stat("damage", Component.literal(String.format("%.1f", (shape.attack() + mat.attackBonus())
                            * com.xtrip.golemgarage.Config.materialStat(mat.id(), "damage")))),
                    stat("work", Component.literal(Math.round(shape.work() * (1 + 0.1f * mat.tier()) * mat.workMult() * 100) + "%")));
            case LEGS -> List.of(
                    stat("speed", Component.literal(Math.round(shape.speed() * mat.speedMult() * 100) + "%")),
                    stat("knockback", Component.literal(Math.round(shape.knockbackResist() * 100) + "%")));
            case CORE -> List.of(
                    stat("power", Component.literal(String.valueOf(Math.round(shape.capacity() * mat.capacityMult())))),
                    stat("recharge", Component.translatable(rechargeText())));
        };
    }

    private String rechargeText() {
        if (shape.has(ShapeAbility.RECHARGE_REDSTONE)) {
            return "tooltip.golemgarage.recharge.redstone";
        } else if (shape.has(ShapeAbility.RECHARGE_FUEL)) {
            return "tooltip.golemgarage.recharge.fuel";
        } else if (shape.has(ShapeAbility.RECHARGE_STORM)) {
            return "tooltip.golemgarage.recharge.storm";
        } else if (shape.has(ShapeAbility.RECHARGE_SUN)) {
            return "tooltip.golemgarage.recharge.sun";
        }
        return "tooltip.golemgarage.recharge.any";
    }

    private static Component stat(String name, Component value) {
        return Component.translatable("tooltip.golemgarage.stat." + name).append(": ").withStyle(ChatFormatting.GRAY)
                .append(value.copy().withStyle(ChatFormatting.WHITE));
    }

    public static Component traitText(GolemTrait trait) {
        return Component.translatable("trait.golemgarage." + trait.name().toLowerCase(java.util.Locale.ROOT));
    }
}
