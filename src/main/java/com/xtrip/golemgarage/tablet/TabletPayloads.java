package com.xtrip.golemgarage.tablet;

import com.xtrip.golemgarage.GolemGarage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Network messages for the Golem Tablet. */
public final class TabletPayloads {
    private TabletPayloads() {
    }

    /** One golem as the tablet shows it. entityId is -1 when it isn't loaded near you. */
    public record Row(int index, String name, String dim, int x, int y, int z, int entityId, float health, float maxHealth,
                      int powerPct, int rank, String status) {
        static void write(FriendlyByteBuf b, Row r) {
            b.writeVarInt(r.index);
            b.writeUtf(r.name);
            b.writeUtf(r.dim);
            b.writeInt(r.x);
            b.writeInt(r.y);
            b.writeInt(r.z);
            b.writeInt(r.entityId);
            b.writeFloat(r.health);
            b.writeFloat(r.maxHealth);
            b.writeVarInt(r.powerPct);
            b.writeVarInt(r.rank);
            b.writeUtf(r.status);
        }

        static Row read(FriendlyByteBuf b) {
            return new Row(b.readVarInt(), b.readUtf(), b.readUtf(), b.readInt(), b.readInt(), b.readInt(), b.readInt(),
                    b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt(), b.readUtf());
        }
    }

    /** Server to client: your golems. */
    public record Data(List<Row> rows) implements CustomPacketPayload {
        public static final Type<Data> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "tablet_data"));
        public static final StreamCodec<FriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.of(
                (b, d) -> {
                    b.writeVarInt(d.rows.size());
                    d.rows.forEach(r -> Row.write(b, r));
                },
                b -> {
                    int n = b.readVarInt();
                    List<Row> rows = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        rows.add(Row.read(b));
                    }
                    return new Data(rows);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: refresh (0), recall (1), stop (2), resume (3) or started watching (4) golem number index. */
    public record Action(int action, int index) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "tablet_action"));
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::action, ByteBufCodecs.VAR_INT, Action::index, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
