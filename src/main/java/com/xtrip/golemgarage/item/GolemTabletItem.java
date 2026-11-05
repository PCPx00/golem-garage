package com.xtrip.golemgarage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The Golem Tablet: a map and list of all your golems, with recall, stop/resume and a live camera. */
public class GolemTabletItem extends Item {
    public GolemTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            com.xtrip.golemgarage.tablet.TabletServer.send(sp);
        } else if (level.isClientSide) {
            com.xtrip.golemgarage.client.ClientHooks.openTablet();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("golemgarage.tablet.tip").withStyle(ChatFormatting.GRAY));
    }
}
