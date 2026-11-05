package com.xtrip.golemgarage.block;

import com.xtrip.golemgarage.power.GeneratorBlockEntity;
import com.xtrip.golemgarage.power.WindTurbineBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** The action-bar readout for generators without a screen. */
public final class GeneratorInfo {
    private GeneratorInfo() {
    }

    public static InteractionResult show(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GeneratorBlockEntity gen) {
            Component msg = Component.translatable("golemgarage.power.readout", gen.rate(), fe(gen.stored()), fe(gen.capacity()))
                    .withStyle(gen.rate() > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY);
            if (gen instanceof WindTurbineBlockEntity wind && wind.clearance() < 0.99f) {
                msg = msg.copy().append(Component.translatable("golemgarage.power.blocked",
                        Math.round(100 * (1 - wind.clearance()))).withStyle(ChatFormatting.GOLD));
            }
            player.displayClientMessage(msg, true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 1234 -> "1,234", 45200 -> "45.2k". */
    public static String fe(int n) {
        if (n >= 1_000_000) {
            return String.format(java.util.Locale.ROOT, "%.1fM", n / 1_000_000f);
        }
        if (n >= 10_000) {
            return String.format(java.util.Locale.ROOT, "%.1fk", n / 1000f);
        }
        return String.format(java.util.Locale.ROOT, "%,d", n);
    }
}
