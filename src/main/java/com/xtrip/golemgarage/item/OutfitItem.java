package com.xtrip.golemgarage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A Cub outfit piece. Right-click a Cub to dress it, or put it in the Cub's wardrobe slots. */
public class OutfitItem extends Item {
    public enum Slot { HEAD, BODY, ACCESSORY }

    private final String outfitId;
    private final Slot slot;
    private final String theme;

    public OutfitItem(String outfitId, Slot slot, String theme, Properties properties) {
        super(properties.stacksTo(1));
        this.outfitId = outfitId;
        this.slot = slot;
        this.theme = theme;
    }

    public String outfitId() {
        return outfitId;
    }

    public Slot slot() {
        return slot;
    }

    public String theme() {
        return theme;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("outfit.golemgarage.tooltip",
                Component.translatable("outfit.golemgarage.theme." + theme),
                Component.translatable("outfit.golemgarage.slot." + slot.name().toLowerCase()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("outfit.golemgarage.set_bonus",
                Component.translatable("outfit.golemgarage.theme." + theme)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
