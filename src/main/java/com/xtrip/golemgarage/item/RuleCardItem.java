package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.golem.GolemRule;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A punch card that holds a golem's rules. Use a blank card on your golem to punch its rules onto the card; use a
 * punched card on another golem to load them. Cards stack when blank, can be copied in a crafting grid with a blank
 * card, and are sold by toolsmiths. Sneak + use in the air wipes a card.
 */
public class RuleCardItem extends Item {
    public RuleCardItem(Properties properties) {
        super(properties);
    }

    public static boolean isPunched(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().contains("Rules");
    }

    public static List<GolemRule> rules(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? List.of() : GolemRule.fromTag(d.copyTag());
    }

    public static ItemStack punched(List<GolemRule> rules, String name) {
        ItemStack card = new ItemStack(ModItems.RULE_CARD.get());
        CompoundTag tag = GolemRule.toTag(rules);
        tag.putString("From", name);
        card.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return card;
    }

    /** Called from the golem's interaction. Returns true if the card did something. */
    public static boolean useOn(GolemEntity golem, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isPunched(held)) {
            ItemStack card = punched(golem.getRules(), golem.getName().getString());
            held.shrink(1);
            if (held.isEmpty()) {
                player.setItemInHand(hand, card);
            } else if (!player.addItem(card)) {
                player.drop(card, false);
            }
            golem.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 1.2f);
            player.displayClientMessage(Component.translatable("golemgarage.card.punched", golem.getName(),
                    golem.getRules().size()).withStyle(ChatFormatting.AQUA), true);
            return true;
        }
        List<GolemRule> rules = rules(held);
        golem.setRules(rules);
        golem.playSound(SoundEvents.UI_LOOM_TAKE_RESULT, 1f, 1.4f);
        player.displayClientMessage(Component.translatable("golemgarage.card.loaded", rules.size(), golem.getName())
                .withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive() && isPunched(held)) {
            if (!level.isClientSide) {
                held.remove(DataComponents.CUSTOM_DATA);
                player.displayClientMessage(Component.translatable("golemgarage.card.wiped"), true);
            }
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
        }
        return InteractionResultHolder.pass(held);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isPunched(stack);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return isPunched(stack) ? 1 : 16;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        if (!isPunched(stack)) {
            tooltip.add(Component.translatable("golemgarage.card.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        String from = d.copyTag().getString("From");
        if (!from.isEmpty()) {
            tooltip.add(Component.translatable("golemgarage.card.from", from).withStyle(ChatFormatting.GRAY));
        }
        for (GolemRule r : rules(stack)) {
            tooltip.add(Component.literal(" " + r.describe()).withStyle(ChatFormatting.DARK_AQUA));
        }
        tooltip.add(Component.translatable("golemgarage.card.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
