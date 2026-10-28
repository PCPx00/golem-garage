package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.entity.ModEntities;
import com.xtrip.golemgarage.golem.GolemData;
import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.golem.PartSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The item form of a golem. Holds its parts, rules, inventory, owner and broken state.
 * A fresh one from the crafting table is a bare frame: place it and fit parts to it.
 */
public class GolemItem extends Item {
    public GolemItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack createStack(GolemEntity golem, boolean broken) {
        CompoundTag tag = new CompoundTag();
        golem.exportData(tag);
        tag.putBoolean("Broken", broken);
        return stackFromTag(tag);
    }

    public static ItemStack stackFromTag(CompoundTag tag) {
        ItemStack stack = new ItemStack(ModItems.GOLEM.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static CompoundTag data(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    public static boolean isBroken(ItemStack stack) {
        return data(stack).getBoolean("Broken");
    }

    /** The golem data if this stack is a broken golem, otherwise null. */
    @Nullable
    public static CompoundTag readBrokenGolem(ItemStack stack) {
        if (!stack.is(ModItems.GOLEM.get())) {
            return null;
        }
        CompoundTag tag = data(stack);
        return tag.getBoolean("Broken") ? tag : null;
    }

    /** The material id shown on the item icon: the torso, or the first part, or the bare frame. */
    public static String iconMaterial(ItemStack stack) {
        Map<PartSlot, PartInstance> parts = GolemData.readParts(data(stack));
        PartInstance p = parts.getOrDefault(PartSlot.TORSO, parts.values().stream().findFirst().orElse(null));
        return p == null ? "frame" : p.visualMaterialId();
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = data(stack);
        if (tag.contains("CustomName")) {
            return Component.literal(tag.getString("CustomName"));
        }
        return Component.translatable(isBroken(stack) ? "item.golemgarage.golem.broken" : "item.golemgarage.golem");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        CompoundTag tag = data(stack);
        if (tag.getBoolean("Broken")) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("golemgarage.msg.golem_broken")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (player != null && level instanceof net.minecraft.server.level.ServerLevel sl
                && !GolemEntity.canPlaceAnother(sl, player.getUUID(), player, context.getClickedPos())) {
            return InteractionResult.FAIL;
        }
        GolemEntity golem = ModEntities.GOLEM.get().create(level);
        if (golem == null) {
            return InteractionResult.FAIL;
        }
        golem.importData(tag);
        if (golem.getOwnerUuid() == null && player != null) {
            golem.setOwnerUuid(player.getUUID());
        }
        Vec3 pos = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        golem.moveTo(pos.x, pos.y, pos.z, player != null ? player.getYRot() + 180f : 0f, 0f);
        golem.setYHeadRot(golem.getYRot());
        level.addFreshEntity(golem);
        com.xtrip.golemgarage.advancement.ModTriggers.award(player, "place_golem");
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        if (tag.getBoolean("Broken")) {
            tooltip.add(Component.translatable("tooltip.golemgarage.broken").withStyle(ChatFormatting.RED));
        }
        Map<PartSlot, PartInstance> parts = GolemData.readParts(tag);
        if (parts.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.golemgarage.frame")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        for (PartSlot slot : PartSlot.values()) {
            PartInstance p = parts.get(slot);
            if (p == null) {
                continue;
            }
            Component name = p.toStack().getHoverName();
            int pct = Math.round(p.condition() * 100);
            ChatFormatting color = p.isBroken() ? ChatFormatting.RED : pct < 50 ? ChatFormatting.GOLD : ChatFormatting.GRAY;
            tooltip.add(Component.literal("- ").append(name).append(" ").append(p.isBroken() ? Component.translatable("tooltip.golemgarage.part_broken") : Component.literal(pct + "%"))
                    .withStyle(color));
        }
    }

    /** Dropped golems and cubs don't despawn unless the server sets itemDespawnMinutes. */
    @Override
    public int getEntityLifespan(ItemStack stack, net.minecraft.world.level.Level level) {
        int minutes = com.xtrip.golemgarage.Config.get(com.xtrip.golemgarage.Config.ITEM_DESPAWN_MINUTES::get, 0);
        return minutes <= 0 ? Integer.MAX_VALUE : minutes * 60 * 20;
    }
}
