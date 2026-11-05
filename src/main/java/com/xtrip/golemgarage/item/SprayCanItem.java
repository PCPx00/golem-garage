package com.xtrip.golemgarage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A spray can that gives a golem a custom finish. Each can covers 16 golems; a water bucket washes it off. */
public class SprayCanItem extends Item {
    public static final String[] FINISHES = {"flames", "hazard", "rust", "camo", "chrome", "tiger"};
    private final String finish;

    public SprayCanItem(String finish, Properties properties) {
        super(properties.durability(16));
        this.finish = finish;
    }

    public String finish() {
        return finish;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("golemgarage.spray.tip").withStyle(ChatFormatting.GRAY));
    }
}
