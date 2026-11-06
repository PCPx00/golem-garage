package com.xtrip.golemgarage.deco;

import com.xtrip.golemgarage.entity.RigEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * A jerry can. Fill it at a Scrap Burner (from its lava tank, or from the fuel waiting in its slot), then use it on a
 * rig to top up the tank. A full can fills a rig from empty.
 */
public class JerryCanItem extends Item {
    public static final int CAPACITY = RigEntity.MAX_FUEL;

    public JerryCanItem(Properties properties) {
        super(properties);
    }

    public static int fuel(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt("Fuel");
    }

    public static void setFuel(ItemStack stack, int fuel) {
        CompoundTag t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (fuel <= 0) {
            t.remove("Fuel");
        } else {
            t.putInt("Fuel", Math.min(CAPACITY, fuel));
        }
        if (t.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return fuel(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * fuel(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.color(0.95f, 0.55f, 0.15f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        int f = fuel(stack);
        tooltip.add(f > 0 ? Component.translatable("golemgarage.jerry_can.fuel", Math.round(100f * f / CAPACITY))
                .withStyle(ChatFormatting.GOLD) : Component.translatable("golemgarage.jerry_can.empty").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("golemgarage.jerry_can.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
