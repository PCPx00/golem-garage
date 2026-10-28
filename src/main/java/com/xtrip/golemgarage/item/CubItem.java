package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.entity.CubEntity;
import com.xtrip.golemgarage.entity.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The item form of a Cub. Holds its owner, name, color and stored items. */
public class CubItem extends Item {
    public CubItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack createStack(CubEntity cub) {
        CompoundTag tag = new CompoundTag();
        cub.exportData(tag);
        ItemStack stack = new ItemStack(ModItems.CUB.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(ModDataComponents.COLOR.get(), cub.getColor().getId());
        return stack;
    }

    public static int colorId(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.COLOR.get(), DyeColor.PINK.getId());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        CubEntity cub = ModEntities.CUB.get().create(level);
        if (cub == null) {
            return InteractionResult.FAIL;
        }
        cub.importData(tag);
        if (!tag.contains("Color")) {
            cub.setColor(DyeColor.byId(colorId(stack)));
        }
        if (cub.getOwnerUuid() == null && player != null) {
            cub.setOwnerUuid(player.getUUID());
        }
        int maxCubs = com.xtrip.golemgarage.Config.get(com.xtrip.golemgarage.Config.MAX_CUBS_PER_PLAYER::get, 0);
        if (maxCubs > 0 && cub.getOwnerUuid() != null && level instanceof net.minecraft.server.level.ServerLevel sl) {
            int count = 0;
            for (net.minecraft.server.level.ServerLevel l : sl.getServer().getAllLevels()) {
                count += l.getEntities(ModEntities.CUB.get(), c -> cub.getOwnerUuid().equals(c.getOwnerUuid())).size();
            }
            if (count >= maxCubs) {
                if (player != null) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "golemgarage.msg.cub_limit", count, maxCubs).withStyle(net.minecraft.ChatFormatting.RED), true);
                }
                return InteractionResult.FAIL;
            }
        }
        Vec3 pos = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        cub.moveTo(pos.x, pos.y, pos.z, player != null ? player.getYRot() + 180f : 0f, 0f);
        level.addFreshEntity(cub);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DyeColor color = DyeColor.byId(colorId(stack));
        tooltip.add(Component.translatable("tooltip.golemgarage.color", Component.translatable("color.minecraft." + color.getName())).withStyle(ChatFormatting.GRAY));
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            int count = data.copyTag().getList("Items", Tag.TAG_COMPOUND).size();
            if (count > 0) {
                tooltip.add(Component.translatable("tooltip.golemgarage.carrying", count).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** Dropped golems and cubs don't despawn unless the server sets itemDespawnMinutes. */
    @Override
    public int getEntityLifespan(ItemStack stack, net.minecraft.world.level.Level level) {
        int minutes = com.xtrip.golemgarage.Config.get(com.xtrip.golemgarage.Config.ITEM_DESPAWN_MINUTES::get, 0);
        return minutes <= 0 ? Integer.MAX_VALUE : minutes * 60 * 20;
    }
}
