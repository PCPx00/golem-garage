package com.xtrip.golemgarage.network;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.block.entity.GarageControllerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server, from the Garage Planner: set the size and plan visibility, auto-build, or report on the room. */
public record GaragePlanPayload(BlockPos pos, int action, int w, int d, int h, boolean show) implements CustomPacketPayload {
    public static final int SET = 0, BUILD = 1, CHECK = 2;
    public static final Type<GaragePlanPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "garage_plan"));

    public static final StreamCodec<ByteBuf, GaragePlanPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GaragePlanPayload::pos,
            ByteBufCodecs.VAR_INT, GaragePlanPayload::action,
            ByteBufCodecs.VAR_INT, GaragePlanPayload::w,
            ByteBufCodecs.VAR_INT, GaragePlanPayload::d,
            ByteBufCodecs.VAR_INT, GaragePlanPayload::h,
            ByteBufCodecs.BOOL, GaragePlanPayload::show,
            GaragePlanPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(GaragePlanPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || player.distanceToSqr(p.pos().getCenter()) > 24 * 24
                    || !player.level().isLoaded(p.pos())
                    || !(player.level().getBlockEntity(p.pos()) instanceof GarageControllerBlockEntity garage)) {
                return;
            }
            switch (p.action()) {
                case SET -> garage.setPlan(p.w(), p.d(), p.h(), p.show());
                case BUILD -> {
                    String err = garage.autoBuild(player);
                    player.displayClientMessage(err == null
                            ? Component.translatable("golemgarage.planner.started").withStyle(ChatFormatting.AQUA)
                            : Component.translatable(err).withStyle(ChatFormatting.RED), true);
                    if (err == null) {
                        com.xtrip.golemgarage.advancement.ModTriggers.award(player, "auto_build");
                    }
                }
                case CHECK -> player.displayClientMessage(garage.describe(), false);
                default -> {
                }
            }
        });
    }
}
