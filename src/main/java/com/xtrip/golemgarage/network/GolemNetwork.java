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
        PayloadRegistrar registrar = event.registrar("4");
        registrar.playToClient(MaterialsPayload.TYPE, MaterialsPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    // A singleplayer client shares the server's list already; only remote clients need this.
                    if (net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() == null) {
                        com.xtrip.golemgarage.golem.GolemMaterials.setExtras(
                                com.xtrip.golemgarage.data.DatapackMaterials.read(payload.data()));
                    }
                }));
        registrar.playToServer(LinkerScrollPayload.TYPE, LinkerScrollPayload.STREAM_CODEC, GolemNetwork::handleLinkerScroll);
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
                case GolemActionPayload.COPY_RULES_TO_GROUP -> copyRulesToGroup(golem, player);
                case GolemActionPayload.SET_MODE -> golem.setMode(com.xtrip.golemgarage.golem.GolemMode.byId(payload.arg()));
                case GolemActionPayload.CLEAR_LINKS -> {
                    for (GolemLinkerItem.Mode m : GolemLinkerItem.Mode.MARKS) {
                        golem.setLink(m, null);
                    }
                }
                default -> {
                }
            }
        });
    }

    /** Gives every golem in the same group (same owner, loaded, same dimension) a copy of this golem's rules. */
    private static void copyRulesToGroup(GolemEntity source, ServerPlayer player) {
        int group = source.getGroup();
        if (group == 0) {
            return;
        }
        int n = 0;
        if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            for (GolemEntity g : sl.getEntities(com.xtrip.golemgarage.entity.ModEntities.GOLEM.get(),
                    g -> g != source && g.getGroup() == group && java.util.Objects.equals(g.getOwnerUuid(), source.getOwnerUuid())
                            && g.canEdit(player))) {
                g.setRules(new java.util.ArrayList<>(source.getRules()));
                n++;
            }
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("golemgarage.msg.group_copied", n, group)
                .withStyle(net.minecraft.ChatFormatting.GREEN), true);
    }

    private static void handleLinkerScroll(LinkerScrollPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            var stack = player.getItemInHand(payload.offhand() ? net.minecraft.world.InteractionHand.OFF_HAND
                    : net.minecraft.world.InteractionHand.MAIN_HAND);
            if (!(stack.getItem() instanceof GolemLinkerItem) || GolemLinkerItem.mode(stack) != GolemLinkerItem.Mode.GROUP) {
                return;
            }
            GolemLinkerItem.scrollGroup(stack, payload.dir() > 0 ? 1 : -1);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tooltip.golemgarage.linker_group",
                    GolemLinkerItem.group(stack)).withStyle(net.minecraft.ChatFormatting.YELLOW), true);
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
