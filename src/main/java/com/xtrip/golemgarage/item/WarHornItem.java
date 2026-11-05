package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.entity.GolemEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The War Horn: sound it to command every one of your golems within 48 blocks, overriding their rules and mode
 * for a while. Sneak + use switches the call: Rally (run to you and guard you, 30 s), Hold (stand fast and fight
 * whatever comes within 8 blocks, 60 s) or Charge (all attack what you're looking at, 20 s).
 */
public class WarHornItem extends Item {
    private static final GolemEntity.Order[] CALLS = {GolemEntity.Order.RALLY, GolemEntity.Order.HOLD, GolemEntity.Order.CHARGE};

    public WarHornItem(Properties properties) {
        super(properties);
    }

    public static GolemEntity.Order call(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        int i = d == null ? 0 : d.copyTag().getInt("Call");
        return CALLS[Math.floorMod(i, CALLS.length)];
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                CompoundTag t = new CompoundTag();
                t.putInt("Call", (call(stack).ordinal()) % CALLS.length);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
                player.displayClientMessage(Component.translatable("golemgarage.horn.set",
                        Component.translatable("golemgarage.horn." + call(stack).name().toLowerCase(java.util.Locale.ROOT)))
                        .withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        GolemEntity.Order order = call(stack);
        LivingEntity target = null;
        if (order == GolemEntity.Order.CHARGE) {
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getLookAngle().scale(40));
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                    new AABB(eye, end).inflate(1.5), e -> e instanceof LivingEntity && e.isAlive()
                            && !(e instanceof GolemEntity g && player.getUUID().equals(g.getOwnerUuid())), 1600);
            if (hit == null || !(hit.getEntity() instanceof LivingEntity le)) {
                player.displayClientMessage(Component.translatable("golemgarage.horn.no_target"), true);
                return InteractionResultHolder.fail(stack);
            }
            target = le;
        }
        int ticks = switch (order) {
            case RALLY -> 600;
            case HOLD -> 1200;
            default -> 400;
        };
        int count = 0;
        for (GolemEntity g : level.getEntitiesOfClass(GolemEntity.class, player.getBoundingBox().inflate(48),
                g -> player.getUUID().equals(g.getOwnerUuid()) && !g.isBroken())) {
            g.giveOrder(order, ticks, target);
            count++;
        }
        level.playSound(null, player, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(order.ordinal() % 8).value(),
                SoundSource.PLAYERS, 16f, 1f);
        player.getCooldowns().addCooldown(this, 100);
        player.displayClientMessage(Component.translatable("golemgarage.horn.sounded",
                Component.translatable("golemgarage.horn." + order.name().toLowerCase(java.util.Locale.ROOT)), count)
                .withStyle(ChatFormatting.GOLD), true);
        com.xtrip.golemgarage.advancement.ModTriggers.award(player, "war_horn");
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("golemgarage.horn.call",
                Component.translatable("golemgarage.horn." + call(stack).name().toLowerCase(java.util.Locale.ROOT)))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("golemgarage.horn.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
