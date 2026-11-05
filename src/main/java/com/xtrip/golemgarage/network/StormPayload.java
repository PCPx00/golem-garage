package com.xtrip.golemgarage.network;

import com.xtrip.golemgarage.GolemGarage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: whether this player is out in a dust storm (for the dusty fog). */
public record StormPayload(boolean on) implements CustomPacketPayload {
    public static final Type<StormPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "storm"));
    public static final StreamCodec<ByteBuf, StormPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StormPayload::on, StormPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
