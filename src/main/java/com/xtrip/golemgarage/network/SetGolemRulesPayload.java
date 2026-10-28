package com.xtrip.golemgarage.network;

import com.xtrip.golemgarage.GolemGarage;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: replace a golem's rules. The payload holds the rules as a compound tag. */
public record SetGolemRulesPayload(int entityId, CompoundTag rules) implements CustomPacketPayload {
    public static final Type<SetGolemRulesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "set_golem_rules"));

    public static final StreamCodec<ByteBuf, SetGolemRulesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetGolemRulesPayload::entityId,
            ByteBufCodecs.COMPOUND_TAG, SetGolemRulesPayload::rules,
            SetGolemRulesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
