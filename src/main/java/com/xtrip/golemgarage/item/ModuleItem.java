package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.golem.GolemModule;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** An upgrade module. Right-click a golem to install it in a free socket (two per golem). */
public class ModuleItem extends Item {
    private final GolemModule module;

    public ModuleItem(GolemModule module, Properties properties) {
        super(properties.stacksTo(16));
        this.module = module;
    }

    public GolemModule module() {
        return module;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.golemgarage." + module.itemId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.golemgarage.module.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
