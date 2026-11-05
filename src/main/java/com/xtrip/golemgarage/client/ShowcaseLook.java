package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.entity.Veterancy;
import com.xtrip.golemgarage.golem.GolemData;
import com.xtrip.golemgarage.golem.PartSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

/** Look at a golem on a Garage showcase shelf to see its name, rank, personality and parts. */
@EventBusSubscriber(modid = GolemGarage.MOD_ID, value = Dist.CLIENT)
public final class ShowcaseLook {
    private record Statue(AABB box, GolemEntity golem) {
    }

    private static final List<Statue> drawn = new ArrayList<>();
    private static final List<Statue> last = new ArrayList<>();
    private static long frame;

    private ShowcaseLook() {
    }

    static void add(AABB box, GolemEntity golem) {
        long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        if (now != frame) {
            last.clear();
            last.addAll(drawn);
            drawn.clear();
            frame = now;
        }
        drawn.add(new Statue(box, golem));
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.options.hideGui || mc.level == null
                || mc.level.getGameTime() - frame > 2) {
            return;
        }
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(mc.player.getViewVector(1f).scale(8));
        double reach = mc.hitResult == null ? 64 : mc.hitResult.getLocation().distanceToSqr(eye) + 0.5;
        Statue hit = null;
        double best = Double.MAX_VALUE;
        List<Statue> all = new ArrayList<>(last);
        all.addAll(drawn);
        for (Statue s : all) {
            var c = s.box().clip(eye, end);
            if (c.isPresent()) {
                double d = c.get().distanceToSqr(eye);
                // Seen through a window: the window block is the hit result, so allow anything up to just past it.
                if (d < best && d <= Math.max(reach, 9)) {
                    best = d;
                    hit = s;
                }
            }
        }
        if (hit == null) {
            return;
        }
        GolemEntity g = hit.golem();
        GuiGraphics gg = event.getGuiGraphics();
        var font = mc.font;
        List<Component> lines = new ArrayList<>();
        lines.add(g.hasCustomName() && g.getCustomName() != null ? g.getCustomName().copy()
                : Component.translatable("entity.golemgarage.golem"));
        lines.add(Component.literal(I18n.get(Veterancy.rankKey(g.rank())) + " · " + g.getXp() + " xp · "
                + I18n.get(g.getPersonality().key())));
        var parts = g.getEquipped();
        for (PartSlot slot : PartSlot.values()) {
            var p = parts.get(slot);
            if (p != null) {
                lines.add(p.toStack().getHoverName());
            }
        }
        int w = 0;
        for (Component l : lines) {
            w = Math.max(w, font.width(l));
        }
        int x = mc.getWindow().getGuiScaledWidth() / 2 + 12;
        int y = mc.getWindow().getGuiScaledHeight() / 2 - lines.size() * 5;
        gg.fill(x - 4, y - 4, x + w + 4, y + lines.size() * 10 + 2, 0xD0101420);
        gg.renderOutline(x - 4, y - 4, w + 8, lines.size() * 10 + 6, 0xFFFFC14A);
        for (int i = 0; i < lines.size(); i++) {
            gg.drawString(font, lines.get(i), x, y + i * 10, i == 0 ? 0xFFFFC14A : i == 1 ? 0xFFB8E0A0 : 0xFFE6ECF4);
        }
    }
}
