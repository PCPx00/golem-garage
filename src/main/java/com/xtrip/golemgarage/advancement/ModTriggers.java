package com.xtrip.golemgarage.advancement;

import com.xtrip.golemgarage.GolemGarage;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, GolemGarage.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, GolemEventTrigger> GOLEM_EVENT =
            TRIGGERS.register("golem_event", GolemEventTrigger::new);

    /** Awards a Golem Garage milestone to a player (does nothing on the client or for null). */
    public static void award(Player player, String event) {
        if (player instanceof ServerPlayer sp) {
            GOLEM_EVENT.get().trigger(sp, event);
        }
    }

    private ModTriggers() {
    }
}
