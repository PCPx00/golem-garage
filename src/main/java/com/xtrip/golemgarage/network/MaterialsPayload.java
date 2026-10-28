package com.xtrip.golemgarage.network;

import com.xtrip.golemgarage.GolemGarage;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: the datapack golem materials, so parts made of them show up right. */
public record MaterialsPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<MaterialsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "materials"));

    public static final StreamCodec<ByteBuf, MaterialsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, MaterialsPayload::data, MaterialsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
