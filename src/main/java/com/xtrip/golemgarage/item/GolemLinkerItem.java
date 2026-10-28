package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.block.GarageControllerBlock;
import com.xtrip.golemgarage.entity.GolemEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Links golems to places. Shift + right-click the air to pick a mode (Post, Chest A, Chest B,
 * Garage), right-click a block to mark it, then right-click a golem to give it every mark.
 * Shift + right-click a golem with the linker to clear its links.
 */
public class GolemLinkerItem extends Item {
    public enum Mode {
        POST("Post", 0x77DD77), CHEST_A("Chest A", 0x77AAFF), CHEST_B("Chest B", 0xFFAA55), GARAGE("Garage", 0xFF7777),
        AREA_1("Area corner 1", 0xD98CFF), AREA_2("Area corner 2", 0xD98CFF), HOME("Home dock", 0x7AF0D8),
        GROUP("Group", 0xFFE27A);

        /** The modes that mark a place (everything except Group). */
        public static final Mode[] MARKS = {POST, CHEST_A, CHEST_B, GARAGE, AREA_1, AREA_2, HOME};

        public final String label;
        public final int color;

        Mode(String label, int color) {
            this.label = label;
            this.color = color;
        }

        String key() {
            return name();
        }

        /** The mode's name in the player's language. */
        public Component title() {
            return Component.translatable("golemgarage.link." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public GolemLinkerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    public static Mode mode(ItemStack stack) {
        int i = data(stack).getInt("Mode");
        Mode[] all = Mode.values();
        return all[Math.floorMod(i, all.length)];
    }

    /** The group number (1-9) the Linker hands out in Group mode. */
    public static int group(ItemStack stack) {
        int g = data(stack).getInt("Group");
        return g < 1 || g > 9 ? 1 : g;
    }

    /** Shift + scroll in Group mode: pick the next or previous group number. */
    public static void scrollGroup(ItemStack stack, int dir) {
        CompoundTag t = data(stack);
        t.putInt("Group", Math.floorMod(group(stack) - 1 + dir, 9) + 1);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    public static BlockPos mark(ItemStack stack, Mode mode) {
        CompoundTag t = data(stack);
        return t.contains(mode.key()) ? BlockPos.of(t.getLong(mode.key())) : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CompoundTag t = data(stack);
                t.putInt("Mode", (t.getInt("Mode") + 1) % Mode.values().length);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
                Mode m = mode(stack);
                player.displayClientMessage(Component.translatable("golemgarage.msg.linker_mode", m.title())
                        .withStyle(s -> s.withColor(m.color)), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos();
        Mode m = mode(stack);
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if ((m == Mode.CHEST_A || m == Mode.CHEST_B) && com.xtrip.golemgarage.golem.Storage.at(level, pos) == null) {
            message(player, Component.translatable("golemgarage.msg.linker_need_storage"), ChatFormatting.RED);
            return InteractionResult.FAIL;
        }
        if (m == Mode.GROUP) {
            message(player, Component.translatable("golemgarage.msg.linker_group_hint", group(stack)), ChatFormatting.YELLOW);
            return InteractionResult.FAIL;
        }
        if (m == Mode.HOME && !(level.getBlockState(pos).getBlock() instanceof com.xtrip.golemgarage.block.GolemDockBlock)) {
            message(player, Component.translatable("golemgarage.msg.linker_need_dock"), ChatFormatting.RED);
            return InteractionResult.FAIL;
        }
        if (m == Mode.GARAGE && !(level.getBlockState(pos).getBlock() instanceof GarageControllerBlock)) {
            message(player, Component.translatable("golemgarage.msg.linker_need_garage"), ChatFormatting.RED);
            return InteractionResult.FAIL;
        }
        BlockPos target = m == Mode.POST || m == Mode.AREA_1 || m == Mode.AREA_2 ? pos.relative(ctx.getClickedFace()) : pos;
        CompoundTag t = data(stack);
        t.putLong(m.key(), target.asLong());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        if (player != null) {
            player.displayClientMessage(Component.translatable("golemgarage.msg.linker_marked", m.title(), target.toShortString()).withStyle(s -> s.withColor(m.color)), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Called by the golem when the player right-clicks it with the linker. */
    public static void applyTo(GolemEntity golem, ItemStack stack, Player player) {
        com.xtrip.golemgarage.advancement.ModTriggers.award(player, "linked");
        if (mode(stack) == Mode.GROUP) {
            int g = player.isShiftKeyDown() ? 0 : group(stack);
            golem.setGroup(g);
            message(player, g == 0 ? Component.translatable("golemgarage.msg.group_cleared")
                    : Component.translatable("golemgarage.msg.group_set", g), ChatFormatting.YELLOW);
            return;
        }
        if (player.isShiftKeyDown()) {
            for (Mode m : Mode.MARKS) {
                golem.setLink(m, null);
            }
            message(player, Component.translatable("golemgarage.msg.linker_cleared"), ChatFormatting.YELLOW);
            return;
        }
        net.minecraft.network.chat.MutableComponent linked = Component.empty();
        boolean any = false;
        for (Mode m : Mode.MARKS) {
            BlockPos pos = mark(stack, m);
            if (pos != null) {
                golem.setLink(m, pos);
                if (any) {
                    linked.append(", ");
                }
                linked.append(m.title());
                any = true;
            }
        }
        if (!any) {
            message(player, Component.translatable("golemgarage.msg.linker_no_marks"), ChatFormatting.RED);
        } else {
            message(player, Component.translatable("golemgarage.msg.linker_linked", linked), ChatFormatting.GREEN);
        }
    }

    private static void message(Player player, Component text, ChatFormatting color) {
        if (player != null) {
            player.displayClientMessage(text.copy().withStyle(color), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Mode current = mode(stack);
        tooltip.add(Component.translatable("tooltip.golemgarage.linker_mode", current.title()).withStyle(s -> s.withColor(current.color)));
        if (current == Mode.GROUP) {
            tooltip.add(Component.translatable("tooltip.golemgarage.linker_group", group(stack)).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("tooltip.golemgarage.linker_group_h").withStyle(ChatFormatting.DARK_GRAY));
        }
        for (Mode m : Mode.MARKS) {
            BlockPos p = mark(stack, m);
            tooltip.add(m.title().copy().append(": " + (p == null ? "-" : p.toShortString()))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.golemgarage.linker_h1").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.golemgarage.linker_h2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.golemgarage.linker_h3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
