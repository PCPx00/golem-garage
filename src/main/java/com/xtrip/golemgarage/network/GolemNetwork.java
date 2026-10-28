package com.xtrip.golemgarage.network;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.golem.GolemRule;
import com.xtrip.golemgarage.golem.PartSlot;
import com.xtrip.golemgarage.item.GolemLinkerItem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = GolemGarage.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class GolemNetwork {
    private static final double MAX_EDIT_DISTANCE = 16.0;

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToServer(SetGolemRulesPayload.TYPE, SetGolemRulesPayload.STREAM_CODEC, GolemNetwork::handleRules);
        registrar.playToServer(GolemActionPayload.TYPE, GolemActionPayload.STREAM_CODEC, GolemNetwork::handleAction);
        registrar.playToServer(PickUpPayload.TYPE, PickUpPayload.STREAM_CODEC, GolemNetwork::handlePickUp);
    }

    /** The golem the player may edit: theirs (or unowned), and within reach. */
    private static GolemEntity editable(IPayloadContext context, int entityId) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return null;
        }
        if (!(player.level().getEntity(entityId) instanceof GolemEntity golem)) {
            return null;
        }
        if (!golem.canEdit(player) || golem.distanceTo(player) > MAX_EDIT_DISTANCE) {
            return null;
        }
        return golem;
    }

    private static void handleRules(SetGolemRulesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            GolemEntity golem = editable(context, payload.entityId());
            if (golem != null) {
                golem.setRules(GolemRule.fromTag(payload.rules()));
                com.xtrip.golemgarage.advancement.ModTriggers.award(context.player(), "rules_set");
            }
        });
    }

    private static void handleAction(GolemActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            GolemEntity golem = editable(context, payload.entityId());
            if (golem == null || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            switch (payload.action()) {
                case GolemActionPayload.EJECT_PART -> {
                    PartSlot[] slots = PartSlot.values();
                    if (payload.arg() >= 0 && payload.arg() < slots.length) {
                        golem.ejectPart(player, slots[payload.arg()]);
                    }
                }
                case GolemActionPayload.SET_POST_HERE -> golem.setLink(GolemLinkerItem.Mode.POST, golem.blockPosition());
                case GolemActionPayload.REMOVE_MODULE -> golem.removeModule(player, payload.arg());
                case GolemActionPayload.CLEAR_LINKS -> {
                    for (GolemLinkerItem.Mode m : GolemLinkerItem.Mode.values()) {
                        golem.setLink(m, null);
                    }
                }
                default -> {
                }
            }
        });
    }

    /** Hotkey pick-up: a golem or cub the player owns (or nobody owns), within 7 blocks. */
    private static void handlePickUp(PickUpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            var entity = player.level().getEntity(payload.entityId());
            if (entity == null || entity.distanceTo(player) > 7.0) {
                return;
            }
            if (entity instanceof GolemEntity golem) {
                golem.tryPickUp(player);
            } else if (entity instanceof com.xtrip.golemgarage.entity.CubEntity cub) {
                cub.tryPickUp(player);
            }
        });
    }

    private GolemNetwork() {
    }
}
