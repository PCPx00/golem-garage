package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.entity.ModEntities;
import com.xtrip.golemgarage.entity.RigEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/** A packed-up Wasteland Rig. Use it on the ground to unpack it, facing the way you're looking. */
public class RigItem extends Item {
    public RigItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        RigEntity rig = ModEntities.RIG.get().create(level);
        if (rig == null) {
            return InteractionResult.FAIL;
        }
        float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0;
        rig.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        if (!level.noCollision(rig, rig.getBoundingBox())) {
            if (ctx.getPlayer() != null) {
                ctx.getPlayer().displayClientMessage(Component.translatable("golemgarage.rig.no_room"), true);
            }
            return InteractionResult.FAIL;
        }
        rig.setYBodyRot(yaw);
        rig.setYHeadRot(yaw);
        rig.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
        rig.fromItem(ctx.getItemInHand());
        if (ctx.getPlayer() != null) {
            rig.setOwner(ctx.getPlayer().getUUID());
        }
        level.addFreshEntity(rig);
        level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.NEUTRAL, 0.5f, 0.7f);
        level.gameEvent(ctx.getPlayer(), GameEvent.ENTITY_PLACE, pos);
        ctx.getItemInHand().consume(1, ctx.getPlayer());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        var data = stack.get(ModDataComponents.RIG_DATA.get());
        int fuel = data == null ? 20 * 60 * 2 : data.copyTag().getInt("Fuel");
        tooltip.add(Component.translatable("golemgarage.rig.fuel", fuel / 1200).withStyle(net.minecraft.ChatFormatting.GRAY));
        if (data != null) {
            byte up = data.copyTag().getByte("Upgrades");
            if ((up & RigEntity.RAM) != 0) {
                tooltip.add(Component.translatable("item.golemgarage.rig_ram").withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
            }
            if ((up & RigEntity.TURRET) != 0) {
                tooltip.add(Component.translatable("item.golemgarage.rig_turret").withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
            }
            if ((up & RigEntity.PLATING) != 0) {
                tooltip.add(Component.translatable("item.golemgarage.rig_plating").withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
            }
        }
        tooltip.add(Component.translatable("golemgarage.rig.tip").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
