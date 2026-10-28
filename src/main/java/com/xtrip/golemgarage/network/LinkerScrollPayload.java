package com.xtrip.golemgarage.network;

import com.xtrip.golemgarage.GolemGarage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: shift + scroll with a Golem Linker in Group mode (dir is +1 or -1). */
public record LinkerScrollPayload(boolean offhand, int dir) implements CustomPacketPayload {
    public static final Type<LinkerScrollPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "linker_scroll"));

    public static final StreamCodec<ByteBuf, LinkerScrollPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, LinkerScrollPayload::offhand, ByteBufCodecs.VAR_INT, LinkerScrollPayload::dir,
            LinkerScrollPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
