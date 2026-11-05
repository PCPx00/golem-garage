package com.xtrip.golemgarage;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * One-time tips for new players at the moments they matter: first part crafted, first golem, first breakdown,
 * first cub, first raid. Each tip shows once per player; the server config can turn them off.
 */
public final class Hints {
    private static final String KEY = "golemgarage_hints";

    private Hints() {
    }

    public static void show(@Nullable Player player, String id) {
        if (player == null || player.level().isClientSide || !Config.HINTS.get()) {
            return;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag seen = persisted.getCompound(KEY);
        if (seen.getBoolean(id)) {
            return;
        }
        seen.putBoolean(id, true);
        persisted.put(KEY, seen);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        player.sendSystemMessage(Component.translatable("golemgarage.hint.prefix").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(" ")).append(Component.translatable("golemgarage.hint." + id)
                        .withStyle(s -> s.withBold(false).withColor(ChatFormatting.YELLOW))));
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.5f, 1.4f);
    }
}
