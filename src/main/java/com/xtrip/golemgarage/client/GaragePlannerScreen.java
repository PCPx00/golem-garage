package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.block.GaragePlan;
import com.xtrip.golemgarage.block.GarageStyle;
import com.xtrip.golemgarage.block.ModBlocks;
import com.xtrip.golemgarage.block.entity.GarageControllerBlockEntity;
import com.xtrip.golemgarage.network.GaragePlanPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Garage Planner, opened by right-clicking a Garage Controller: pick the room's inside size and style, show or
 * hide its plan in the world, see what it takes to build, auto-build it, and open the golem showcase.
 */
public class GaragePlannerScreen extends Screen {
    private static final int W = 300, H = 220;
    private static final int BG = 0xF0141A22, PANEL = 0xFF0B1016, EDGE = 0xFF3A4A5E, TEXT = 0xFFE6ECF4, DIM = 0xFF8A96A8,
            ACCENT = 0xFF5FE8FF, GOLD = 0xFFFFC14A, GOOD = 0xFF6CFF8A, BAD = 0xFFFF6A6A;
    private final BlockPos pos;
    private int w, d, h;
    private boolean show;
    private GarageStyle style;
    private GaragePlan.Needs needs;
    private int recheck;
    private Button build;

    public GaragePlannerScreen(BlockPos pos) {
        super(Component.translatable("golemgarage.planner.title"));
        this.pos = pos;
        GarageControllerBlockEntity g = garage();
        w = g == null ? 5 : g.planW();
        d = g == null ? 5 : g.planD();
        h = g == null ? 4 : g.planH();
        show = g == null || g.showPlan();
        style = g == null ? GarageStyle.WASTELAND : g.style();
    }

    private GarageControllerBlockEntity garage() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof GarageControllerBlockEntity g ? g : null;
    }

    private int x0() {
        return (width - W) / 2;
    }

    private int y0() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        int x0 = x0(), y0 = y0();
        GarageControllerBlockEntity g = garage();
        boolean locked = g != null && (g.isFormed() || g.buildProgress() >= 0);
        int[] ys = {42, 64, 86};
        for (int i = 0; i < 3; i++) {
            int which = i;
            Button minus = Button.builder(Component.literal("-"), b -> change(which, -1))
                    .bounds(x0 + 70, y0 + ys[i] - 4, 16, 16).build();
            Button plus = Button.builder(Component.literal("+"), b -> change(which, 1))
                    .bounds(x0 + 118, y0 + ys[i] - 4, 16, 16).build();
            minus.active = !locked && value(which) > min(which);
            plus.active = !locked && value(which) < max(which);
            addRenderableWidget(minus);
            addRenderableWidget(plus);
        }
        Button styleButton = addRenderableWidget(Button.builder(Component.translatable("golemgarage.planner.style",
                Component.translatable(style.key())), b -> {
            style = style.next();
            GaragePlanOverlay.previewStyle = style;
            send(GaragePlanPayload.SET);
            rebuildWidgets();
        }).bounds(x0 + 10, y0 + 152, 132, 18).tooltip(Tooltip.create(Component.translatable(style.key() + ".about"))).build());
        styleButton.active = g == null || !g.isFormed();
        addRenderableWidget(Button.builder(Component.translatable(show ? "golemgarage.planner.shown" : "golemgarage.planner.hidden"), b -> {
            show = !show;
            send(GaragePlanPayload.SET);
            rebuildWidgets();
        }).bounds(x0 + 10, y0 + 173, 132, 18).tooltip(Tooltip.create(Component.translatable("golemgarage.planner.show_tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("golemgarage.planner.check"), b -> {
            send(GaragePlanPayload.CHECK);
            onClose();
        }).bounds(x0 + 10, y0 + 194, 132, 18).build());
        int slots = g == null ? 1 : g.showcaseSlots();
        addRenderableWidget(Button.builder(Component.translatable("golemgarage.planner.showcase", slots), b ->
                send(GaragePlanPayload.SHOWCASE)).bounds(x0 + 156, y0 + 173, 134, 18)
                .tooltip(Tooltip.create(Component.translatable("golemgarage.planner.showcase_tip"))).build());
        build = addRenderableWidget(Button.builder(Component.translatable("golemgarage.planner.build"), b -> {
            send(GaragePlanPayload.BUILD);
            onClose();
        }).bounds(x0 + 156, y0 + 194, 134, 18).build());
        recheck = 0;
        refresh();
    }

    private int value(int which) {
        return which == 0 ? w : which == 1 ? d : h;
    }

    private static int min(int which) {
        return which == 0 ? GaragePlan.MIN_W : which == 1 ? GaragePlan.MIN_D : GaragePlan.MIN_H;
    }

    private static int max(int which) {
        return which == 0 ? GaragePlan.MAX_W : which == 1 ? GaragePlan.MAX_D : GaragePlan.MAX_H;
    }

    private void change(int which, int dir) {
        switch (which) {
            case 0 -> w = GaragePlan.clampWidth(w + dir * 2);
            case 1 -> d = GaragePlan.clampDepth(d + dir);
            default -> h = GaragePlan.clampHeight(h + dir);
        }
        send(GaragePlanPayload.SET);
        rebuildWidgets();
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new GaragePlanPayload(pos, action, w, d, h, show, style.ordinal()));
    }

    /** Re-reads the world and the inventory for the materials list. */
    private void refresh() {
        var mc = Minecraft.getInstance();
        GarageControllerBlockEntity g = garage();
        if (g == null || mc.player == null || mc.level == null) {
            needs = null;
            return;
        }
        var f = g.getBlockState().getValue(com.xtrip.golemgarage.block.GarageControllerBlock.FACING);
        needs = GaragePlan.needs(GaragePlan.plan(mc.level, pos, f, w, d, h, style), mc.player);
        if (build != null) {
            boolean busy = g.buildProgress() >= 0;
            build.active = !g.isFormed() && !busy && needs.canBuild();
            build.setTooltip(Tooltip.create(Component.translatable(buildTip(g, busy))));
        }
    }

    private String buildTip(GarageControllerBlockEntity g, boolean busy) {
        if (g.isFormed()) {
            return "golemgarage.planner.err.formed";
        }
        if (busy) {
            return "golemgarage.planner.err.busy";
        }
        if (needs.blocked() > 0) {
            return "golemgarage.planner.err.blocked";
        }
        if (needs.total() == 0) {
            return "golemgarage.planner.nothing";
        }
        return needs.canBuild() ? "golemgarage.planner.build_tip" : "golemgarage.planner.err.materials";
    }

    @Override
    public void tick() {
        GarageControllerBlockEntity g = garage();
        if (g == null) {
            onClose();
            return;
        }
        if (++recheck % 10 == 0) {
            refresh();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);
        int x0 = x0(), y0 = y0();
        g.fill(x0, y0, x0 + W, y0 + H, BG);
        g.renderOutline(x0, y0, W, H, EDGE);
        for (int x = x0 + 4; x < x0 + W - 4; x++) {
            int c = ((x - x0) / 4) % 2 == 0 ? 0xFFDEAC2E : 0xFF1E1C1E;
            g.fill(x, y0 + 20, x + 1, y0 + 22, c);
        }
        g.fill(x0 + 150, y0 + 26, x0 + 151, y0 + H - 6, EDGE);
        g.fill(x0 + 156, y0 + 38, x0 + 290, y0 + 124, PANEL);
        g.renderOutline(x0 + 156, y0 + 38, 134, 86, EDGE);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int x0 = x0(), y0 = y0();
        GarageControllerBlockEntity ge = garage();
        g.drawString(font, title, x0 + 10, y0 + 8, ACCENT);
        String size = I18n.get("golemgarage.planner.size", w, d, h);
        g.drawString(font, size, x0 + W - 10 - font.width(size), y0 + 8, DIM);

        // Left: the size and what it makes.
        g.drawString(font, Component.translatable("golemgarage.planner.inside"), x0 + 10, y0 + 28, DIM);
        String[] labels = {"golemgarage.planner.width", "golemgarage.planner.depth", "golemgarage.planner.height"};
        int[] ys = {42, 64, 86};
        for (int i = 0; i < 3; i++) {
            g.drawString(font, Component.translatable(labels[i]), x0 + 10, y0 + ys[i], TEXT);
            String v = String.valueOf(value(i));
            g.drawString(font, v, x0 + 102 - font.width(v) / 2, y0 + ys[i], GOLD);
        }
        int tier = w >= 7 && d >= 7 ? 3 : w >= 5 && d >= 5 ? 2 : 1;
        g.drawString(font, Component.translatable("golemgarage.tier." + tier), x0 + 10, y0 + 108, tier == 3 ? GOLD : tier == 2 ? ACCENT : TEXT);
        int ty = y0 + 120;
        for (FormattedCharSequence l : font.split(Component.translatable("golemgarage.planner.tier_hint." + tier), 132)) {
            if (ty > y0 + 140) {
                break;
            }
            g.drawString(font, l, x0 + 10, ty, DIM);
            ty += 10;
        }

        // Right: what it takes.
        g.drawString(font, Component.translatable("golemgarage.planner.materials"), x0 + 156, y0 + 28, DIM);
        if (ge != null && ge.isFormed()) {
            int y = y0 + 44;
            for (FormattedCharSequence l : font.split(Component.translatable("golemgarage.planner.working"), 124)) {
                g.drawString(font, l, x0 + 161, y, GOOD);
                y += 10;
            }
        } else if (needs != null) {
            row(g, x0 + 160, y0 + 42, new ItemStack(Items.STONE_BRICKS), "golemgarage.planner.walls", needs.walls(), needs.haveWalls());
            row(g, x0 + 160, y0 + 62, new ItemStack(Items.GLASS), "golemgarage.planner.windows", needs.windows(), needs.haveWindows());
            row(g, x0 + 160, y0 + 82, new ItemStack(Items.SMOOTH_STONE), "golemgarage.planner.floor", needs.solid(), needs.haveSolid());
            row(g, x0 + 160, y0 + 102, new ItemStack(ModBlocks.LASER_EMITTER.get()), "golemgarage.planner.emitters",
                    needs.emitters(), needs.haveEmitters());
        }
        int sy = y0 + 130;
        Component status;
        int col;
        float prog = ge == null ? -1 : ge.buildProgress();
        if (prog >= 0) {
            status = Component.translatable("golemgarage.planner.building", Math.round(prog * 100));
            col = ACCENT;
            g.fill(x0 + 156, y0 + 162, x0 + 290, y0 + 168, PANEL);
            g.fill(x0 + 156, y0 + 162, x0 + 156 + Math.round(134 * prog), y0 + 168, ACCENT);
        } else if (ge != null && ge.isFormed() || needs == null) {
            status = Component.empty();
            col = GOOD;
        } else if (needs.blocked() > 0) {
            status = Component.translatable("golemgarage.planner.blocked", needs.blocked());
            col = BAD;
        } else if (needs.total() == 0) {
            status = Component.translatable("golemgarage.planner.nothing");
            col = GOOD;
        } else if (needs.canBuild()) {
            status = Component.translatable(Minecraft.getInstance().player != null
                    && Minecraft.getInstance().player.getAbilities().instabuild ? "golemgarage.planner.ready_creative"
                    : "golemgarage.planner.ready");
            col = GOOD;
        } else {
            status = Component.translatable("golemgarage.planner.missing");
            col = GOLD;
        }
        for (FormattedCharSequence l : font.split(status, 134)) {
            if (sy > y0 + 150) {
                break;
            }
            g.drawString(font, l, x0 + 156, sy, col);
            sy += 10;
        }
    }

    /** A materials row: icon, name, and need/have, green when there's enough. */
    private void row(GuiGraphics g, int x, int y, ItemStack icon, String key, int need, int have) {
        g.renderItem(icon, x, y);
        boolean creative = Minecraft.getInstance().player != null && Minecraft.getInstance().player.getAbilities().instabuild;
        String right = need == 0 ? I18n.get("golemgarage.plan.done") : creative ? String.valueOf(need) : need + "/" + have;
        int rc = need == 0 || creative || have >= need ? GOOD : BAD;
        int rw = font.width(right);
        g.drawString(font, font.plainSubstrByWidth(I18n.get(key), 124 - 20 - rw - 4), x + 20, y + 4, need == 0 ? DIM : TEXT);
        g.drawString(font, right, x + 126 - rw, y + 4, rc);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
