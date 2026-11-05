package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.golem.GolemText;
import com.xtrip.golemgarage.tablet.TabletPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The Golem Tablet screen: a radar map of your golems around you on the left, the list on the right, and buttons
 * to recall, stop, resume or watch the selected golem through its own eyes.
 */
public class TabletScreen extends Screen {
    private static final int W = 360, H = 220;
    private static final int BG = 0xF0141A22, PANEL = 0xFF0B1016, EDGE = 0xFF3A4A5E, TEXT = 0xFFE6ECF4, DIM = 0xFF8A96A8,
            ACCENT = 0xFF5FE8FF, GOLD = 0xFFFFC14A;
    private static List<TabletPayloads.Row> rows = List.of();
    private int selected;
    private int scroll;

    public TabletScreen() {
        super(Component.translatable("item.golemgarage.golem_tablet"));
    }

    /** Fresh data from the server. */
    public static void receive(TabletPayloads.Data data) {
        rows = data.rows();
    }

    private int x0() {
        return (width - W) / 2;
    }

    private int y0() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        int x = x0() + 186, y = y0() + H - 26;
        addRenderableWidget(Button.builder(Component.translatable("golemgarage.tablet.recall"), b -> act(1)).bounds(x, y, 54, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("golemgarage.tablet.view"), b -> view()).bounds(x + 56, y, 50, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("golemgarage.tablet.stop"), b -> act(2)).bounds(x + 108, y, 30, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("golemgarage.tablet.go"), b -> act(3)).bounds(x + 140, y, 30, 18).build());
    }

    private TabletPayloads.Row sel() {
        return selected >= 0 && selected < rows.size() ? rows.get(selected) : null;
    }

    private void act(int action) {
        TabletPayloads.Row r = sel();
        if (r != null) {
            PacketDistributor.sendToServer(new TabletPayloads.Action(action, r.index()));
        }
    }

    private void view() {
        TabletPayloads.Row r = sel();
        Minecraft mc = Minecraft.getInstance();
        Entity e = r == null || r.entityId() < 0 || mc.level == null ? null : mc.level.getEntity(r.entityId());
        if (e == null) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable("golemgarage.tablet.too_far"), true);
            }
            return;
        }
        PacketDistributor.sendToServer(new TabletPayloads.Action(4, r.index()));
        TabletCamera.watch(e);
        onClose();
    }

    @Override
    public void tick() {
        if (minecraft != null && minecraft.level != null && minecraft.level.getGameTime() % 20 == 0) {
            PacketDistributor.sendToServer(new TabletPayloads.Action(0, 0));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int lx = x0() + 186, ly = y0() + 24;
        if (mx >= lx && mx < lx + 166 && my >= ly && my < ly + 8 * 20) {
            int i = (int) ((my - ly) / 20) + scroll;
            if (i < rows.size()) {
                selected = i;
                return true;
            }
        }
        // Clicking a dot on the map picks that golem.
        int cx = x0() + 90, cy = y0() + 112;
        double scale = mapScale();
        var p = Minecraft.getInstance().player;
        for (int i = 0; p != null && i < rows.size(); i++) {
            TabletPayloads.Row r = rows.get(i);
            double dx = cx + (r.x() - p.getX()) * scale, dy = cy + (r.z() - p.getZ()) * scale;
            if (Math.abs(mx - dx) < 5 && Math.abs(my - dy) < 5) {
                selected = i;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll = Math.max(0, Math.min(Math.max(0, rows.size() - 8), scroll - (int) Math.signum(dy)));
        return true;
    }

    private double mapScale() {
        var p = Minecraft.getInstance().player;
        double far = 24;
        for (TabletPayloads.Row r : rows) {
            if (p != null) {
                far = Math.max(far, Math.max(Math.abs(r.x() - p.getX()), Math.abs(r.z() - p.getZ())));
            }
        }
        return 78.0 / Math.min(far * 1.1, 2000);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);
        int x0 = x0(), y0 = y0();
        g.fill(x0, y0, x0 + W, y0 + H, BG);
        g.renderOutline(x0, y0, W, H, EDGE);
        g.drawString(font, title, x0 + 8, y0 + 8, ACCENT);

        // Radar map.
        int mx0 = x0 + 8, my0 = y0 + 24, ms = 164;
        g.fill(mx0, my0, mx0 + ms, my0 + ms + 20, PANEL);
        g.renderOutline(mx0, my0, ms, ms + 20, EDGE);
        int cx = x0 + 90, cy = y0 + 112;
        for (int r = 20; r < 90; r += 20) {
            ring(g, cx, cy, r, 0x30FFFFFF);
        }
        long t = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        double sweep = (t % 60) / 60.0 * Math.PI * 2;
        for (int k = 0; k < 78; k++) {
            g.fill((int) (cx + Math.cos(sweep) * k), (int) (cy + Math.sin(sweep) * k),
                    (int) (cx + Math.cos(sweep) * k) + 1, (int) (cy + Math.sin(sweep) * k) + 1, 0x705FE8FF);
        }
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);
        var p = Minecraft.getInstance().player;
        double scale = mapScale();
        for (int i = 0; p != null && i < rows.size(); i++) {
            TabletPayloads.Row r = rows.get(i);
            int dx = (int) (cx + (r.x() - p.getX()) * scale), dy = (int) (cy + (r.z() - p.getZ()) * scale);
            dx = Math.max(mx0 + 3, Math.min(mx0 + ms - 3, dx));
            dy = Math.max(my0 + 3, Math.min(my0 + ms - 3, dy));
            boolean problem = GolemText.isProblem(r.status());
            int col = i == selected ? 0xFFFFFFFF : r.entityId() < 0 ? 0xFF6A7488 : problem ? 0xFFFF6A6A : 0xFF5AFF8A;
            g.fill(dx - 2, dy - 2, dx + 3, dy + 3, col);
        }
        g.drawCenteredString(font, Component.translatable("golemgarage.tablet.range", (int) (78 / scale)), cx, my0 + ms + 6, DIM);

        // List.
        int lx = x0 + 186, ly = y0 + 24;
        g.drawString(font, Component.translatable("golemgarage.tablet.count", rows.size()), lx, y0 + 8, DIM);
        for (int k = 0; k < 8 && k + scroll < rows.size(); k++) {
            int i = k + scroll;
            TabletPayloads.Row r = rows.get(i);
            int y = ly + k * 20;
            g.fill(lx, y, lx + 166, y + 18, i == selected ? 0xFF22324A : PANEL);
            String name = font.plainSubstrByWidth(r.name(), 100) + (r.rank() > 0 ? " " + "^".repeat(r.rank()) : "");
            g.drawString(font, name, lx + 4, y + 2, r.rank() > 0 ? GOLD : TEXT);
            int dist = p == null ? 0 : (int) Math.sqrt(Math.pow(r.x() - p.getX(), 2) + Math.pow(r.z() - p.getZ(), 2));
            g.drawString(font, dist + "m", lx + 162 - font.width(dist + "m"), y + 2, DIM);
            String status = GolemText.show(r.status()).getString();
            g.drawString(font, font.plainSubstrByWidth(status, 120), lx + 4, y + 10, GolemText.isProblem(r.status()) ? 0xFFFF8A6A : DIM);
            if (r.maxHealth() > 0) {
                int bw = 36;
                g.fill(lx + 126, y + 12, lx + 126 + bw, y + 15, 0xFF000000);
                g.fill(lx + 126, y + 12, lx + 126 + Math.round(bw * r.health() / r.maxHealth()), y + 15, 0xFF59E07A);
            }
        }
        if (rows.isEmpty()) {
            g.drawString(font, I18n.get("golemgarage.tablet.none"), lx + 4, ly + 4, DIM);
        }
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int a = 0; a < 360; a += 4) {
            double rad = Math.toRadians(a);
            int x = (int) (cx + Math.cos(rad) * r), y = (int) (cy + Math.sin(rad) * r);
            g.fill(x, y, x + 1, y + 1, col);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
