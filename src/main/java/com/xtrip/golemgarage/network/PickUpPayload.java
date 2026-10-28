package com.xtrip.golemgarage.network;

import com.xtrip.golemgarage.GolemGarage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the pick-up hotkey was pressed while looking at a golem or cub. */
public record PickUpPayload(int entityId) implements CustomPacketPayload {
    public static final Type<PickUpPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "pick_up"));

    public static final StreamCodec<ByteBuf, PickUpPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PickUpPayload::entityId, PickUpPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
