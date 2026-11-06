package com.xtrip.golemgarage.deco;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/** A medal a golem earns: a Legend Medal when it reaches Legend, an Arena Medal every fifth arena win. */
public class MedalItem extends Item {
    public MedalItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /** A medal engraved with the golem's name and what it did, handed to the owner (or dropped beside the golem). */
    public static void award(Item medal, net.minecraft.world.entity.Entity golem, Player owner, Component line) {
        ItemStack stack = new ItemStack(medal);
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                golem.getDisplayName().copy().withStyle(s -> s.withColor(ChatFormatting.GOLD).withItalic(false)),
                line.copy().withStyle(s -> s.withColor(ChatFormatting.GRAY).withItalic(false)))));
        Component name = stack.getHoverName();
        if (owner != null && owner.isAlive() && owner.level() == golem.level() && owner.addItem(stack)) {
            owner.displayClientMessage(Component.translatable("golemgarage.medal.got", name,
                    golem.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
            return;
        }
        golem.spawnAtLocation(stack);
    }
}
