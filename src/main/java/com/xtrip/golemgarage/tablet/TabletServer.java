package com.xtrip.golemgarage.tablet;

import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.golem.GolemRegistry;
import com.xtrip.golemgarage.golem.GolemText;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Answers the tablet: lists the player's golems and runs its buttons through the /golemgarage commands. */
public final class TabletServer {
    private TabletServer() {
    }

    public static void handle(TabletPayloads.Action a, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) {
                return;
            }
            String cmd = switch (a.action()) {
                case 1 -> "golemgarage recall " + a.index();
                case 2 -> "golemgarage stop " + a.index();
                case 3 -> "golemgarage resume " + a.index();
                default -> null;
            };
            if (cmd != null) {
                p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), cmd);
            }
            if (a.action() == 4) {
                com.xtrip.golemgarage.advancement.ModTriggers.award(p, "tablet_camera");
            }
            send(p);
        });
    }

    public static void send(ServerPlayer p) {
        List<TabletPayloads.Row> rows = new ArrayList<>();
        int i = 0;
        for (GolemRegistry.Entry e : GolemRegistry.get(p.server).forOwner(p.getUUID())) {
            i++;
            ServerLevel lvl = p.server.getLevel(e.dimension());
            GolemEntity g = lvl == null ? null : lvl.getEntity(e.golem()) instanceof GolemEntity ge ? ge : null;
            String name = g != null ? g.getName().getString() : e.name().isEmpty() ? "Golem" : e.name();
            int id = g != null && g.level() == p.level() && g.distanceTo(p) < 128 ? g.getId() : -1;
            int pct = g == null || g.stats().capacity <= 0 ? -1 : Math.round(100f * g.readSyncedPower() / g.stats().capacity);
            String status = g == null ? GolemText.of("tablet_unloaded") : g.readSyncedStatus();
            var pos = g != null ? g.blockPosition() : e.pos();
            rows.add(new TabletPayloads.Row(i, name, e.dimension().location().getPath(), pos.getX(), pos.getY(), pos.getZ(), id,
                    g == null ? 0 : g.getHealth(), g == null ? 0 : g.getMaxHealth(), pct, g == null ? 0 : g.rank(), status));
        }
        PacketDistributor.sendToPlayer(p, new TabletPayloads.Data(rows));
    }
}
