package com.xtrip.golemgarage.network;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.RigEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: a rig gadget key (harpoon, smoke, spikes, headlights, roof rack) pressed while aboard a rig. */
public record RigActionPayload(int action) implements CustomPacketPayload {
    public static final Type<RigActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "rig_action"));
    public static final StreamCodec<ByteBuf, RigActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RigActionPayload::action, RigActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RigActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof RigEntity rig) {
                rig.gadget(payload.action(), player);
            }
        });
    }
}
