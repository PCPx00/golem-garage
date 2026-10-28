package com.xtrip.golemgarage.network;

import com.xtrip.golemgarage.GolemGarage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: a button in the golem screen (remove a part, set post here, clear links). */
public record GolemActionPayload(int entityId, int action, int arg) implements CustomPacketPayload {
    public static final int EJECT_PART = 0;
    public static final int SET_POST_HERE = 1;
    public static final int CLEAR_LINKS = 2;
    public static final int REMOVE_MODULE = 3;
    public static final int COPY_RULES_TO_GROUP = 4;
    public static final int SET_MODE = 5;

    public static final Type<GolemActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "golem_action"));

    public static final StreamCodec<ByteBuf, GolemActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GolemActionPayload::entityId,
            ByteBufCodecs.VAR_INT, GolemActionPayload::action,
            ByteBufCodecs.VAR_INT, GolemActionPayload::arg,
            GolemActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
