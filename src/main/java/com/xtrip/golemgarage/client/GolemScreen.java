package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.golem.GolemRule;
import com.xtrip.golemgarage.golem.GolemStats;
import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.golem.PartSlot;
import com.xtrip.golemgarage.item.GolemLinkerItem;
import com.xtrip.golemgarage.network.GolemActionPayload;
import com.xtrip.golemgarage.network.SetGolemRulesPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The golem's status panel and rule editor. Two tabs: Status (3D preview, parts, stats, log, links)
 * and Rules (WHEN / DO / UNLESS editor with presets). Rule edits are sent when you press Save.
 */
public class GolemScreen extends Screen {
    private static final int W = 420;
    private static int H = 240;

    private static final int BG = 0xF0141820;
    private static final int PANEL = 0xFF1E2430;
    private static final int EDGE = 0xFF3A4458;
    private static final int TEXT = 0xFFE8ECF2;
    private static final int DIM = 0xFF8C96A8;
    private static final int ACCENT = 0xFF6FD3FF;

    private final GolemEntity golem;
    private final List<GolemRule> rules;
    private boolean rulesTab;
    private boolean dirty;

    public GolemScreen(GolemEntity golem) {
        super(Component.translatable("entity.golemgarage.golem"));
        this.golem = golem;
        this.rules = new ArrayList<>(golem.readSyncedRules());
    }

    private int x0() {
        return (width - W) / 2;
    }

    private int y0() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        H = Math.max(200, Math.min(256, height - 8));
        int x0 = x0();
        int y0 = y0();
        addRenderableWidget(Button.builder(Component.translatable("screen.golemgarage.golem.status_tab"), b -> setTab(false))
                .bounds(x0 + 8, y0 + 6, 70, 16).build()).active = rulesTab;
        addRenderableWidget(Button.builder(Component.translatable("screen.golemgarage.golem.rules_tab"), b -> setTab(true))
                .bounds(x0 + 82, y0 + 6, 70, 16).build()).active = !rulesTab;
        addRenderableWidget(Button.builder(dirty ? Component.translatable("screen.golemgarage.golem.save_close") : Component.translatable("screen.golemgarage.golem.close"), b -> onClose())
                .bounds(x0 + W - 88, y0 + 6, 80, 16).build());
        if (rulesTab) {
            initRules(x0, y0);
        } else {
            initStatus(x0, y0);
        }
    }

    private void setTab(boolean rulesTab) {
        this.rulesTab = rulesTab;
        rebuildWidgets();
    }

    // ================================================================= status tab
    private void initStatus(int x0, int y0) {
        int px = x0 + 146;
        for (int i = 0; i < PartSlot.values().length; i++) {
            PartSlot slot = PartSlot.values()[i];
            int y = y0 + 32 + i * 30;
            if (golem.getPart(slot) != null) {
                final int idx = i;
                addRenderableWidget(Button.builder(Component.literal("x"), b -> {
                    PacketDistributor.sendToServer(new GolemActionPayload(golem.getId(), GolemActionPayload.EJECT_PART, idx));
                }).bounds(px + 132, y + 2, 12, 12).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("screen.golemgarage.golem.remove_part"))).build());
            }
        }
        // Module sockets: a remove button on each filled one.
        var mods = golem.getModules();
        for (int i = 0; i < mods.size(); i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal("x"), b ->
                    PacketDistributor.sendToServer(new GolemActionPayload(golem.getId(), GolemActionPayload.REMOVE_MODULE, idx)))
                    .bounds(x0 + 146 + i * 75 + 60, y0 + 198, 12, 12)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.golemgarage.golem.remove_module")))
                    .build());
        }
        int bx = x0 + 300;
        addRenderableWidget(Button.builder(Component.translatable("screen.golemgarage.golem.post_here"), b ->
                PacketDistributor.sendToServer(new GolemActionPayload(golem.getId(), GolemActionPayload.SET_POST_HERE, 0)))
                .bounds(bx, y0 + H - 26, 54, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.golemgarage.golem.clear_links"), b ->
                PacketDistributor.sendToServer(new GolemActionPayload(golem.getId(), GolemActionPayload.CLEAR_LINKS, 0)))
                .bounds(bx + 58, y0 + H - 26, 54, 16).build());
    }

    private void renderStatus(GuiGraphics g, int x0, int y0, int mouseX, int mouseY) {
        // 3D preview.
        g.fill(x0 + 8, y0 + 28, x0 + 138, y0 + H - 8, PANEL);
        g.renderOutline(x0 + 8, y0 + 28, 130, H - 36, EDGE);
        GuiPreview.render(g, x0 + 10, y0 + 30, x0 + 136, y0 + H - 10, 30, mouseX, mouseY, golem);
        String name = golem.hasCustomName() && golem.getCustomName() != null ? golem.getCustomName().getString() : I18n.get("entity.golemgarage.golem");
        g.drawCenteredString(font, name, x0 + 73, y0 + 32, TEXT);
        if (golem.getGroup() > 0) {
            g.drawCenteredString(font, I18n.get("screen.golemgarage.golem.group", golem.getGroup()), x0 + 73, y0 + 42, 0xFFFFE27A);
        }

        // Parts.
        int px = x0 + 146;
        g.drawString(font, I18n.get("screen.golemgarage.golem.parts"), px, y0 + 22, ACCENT);
        for (int i = 0; i < PartSlot.values().length; i++) {
            PartSlot slot = PartSlot.values()[i];
            int y = y0 + 32 + i * 30;
            g.fill(px, y, px + 148, y + 26, PANEL);
            PartInstance p = golem.getPart(slot);
            String slotName = I18n.get("golemgarage.slot." + slot.id());
            if (p == null) {
                g.drawString(font, I18n.get("screen.golemgarage.golem.slot_empty", slotName), px + 22, y + 4, DIM);
                g.drawString(font, fit(I18n.get("screen.golemgarage.golem.fit_hint"), 124), px + 22, y + 14, 0xFF5E6676);
                continue;
            }
            ItemStack stack = p.toStack();
            g.renderItem(stack, px + 3, y + 5);
            String label = fit(stack.getHoverName().getString(), 106);
            g.drawString(font, label, px + 22, y + 4, p.isBroken() ? 0xFFFF6B6B : TEXT);
            int barW = 98;
            int fill = Math.round(barW * p.condition());
            int col = p.isBroken() ? 0xFFFF4D4D : p.condition() < 0.5f ? 0xFFFFB13B : 0xFF59E07A;
            g.fill(px + 22, y + 16, px + 22 + barW, y + 19, 0xFF0B0E14);
            g.fill(px + 22, y + 16, px + 22 + fill, y + 19, col);
            if (p.oxidation() > 0 || p.waxed()) {
                g.drawString(font, p.waxed() ? I18n.get("screen.golemgarage.golem.wax") : I18n.get("screen.golemgarage.golem.ox", p.oxidation()), px + 124, y + 14, 0xFF5BC9A8);
            }
        }

        // Stats, status, log, links.
        int sx = x0 + 300;
        GolemStats st = golem.stats();
        g.drawString(font, I18n.get("screen.golemgarage.golem.stats"), sx, y0 + 22, ACCENT);
        int y = y0 + 32;
        bar(g, sx, y, I18n.get("screen.golemgarage.golem.health"), golem.getHealth(), golem.getMaxHealth(), 0xFFE0505A);
        y += 18;
        int cap = golem.readSyncedCapacity();
        if (cap > 0) {
            bar(g, sx, y, I18n.get("screen.golemgarage.golem.power"), golem.readSyncedPower(), cap, 0xFF5AB4FF);
        } else {
            g.drawString(font, I18n.get("screen.golemgarage.golem.no_core"), sx, y, DIM);
        }
        y += 18;
        line(g, sx, y, I18n.get("screen.golemgarage.golem.armor_dmg", golem.getArmorValue(), String.format("%.0f", st.attack)));
        y += 10;
        line(g, sx, y, I18n.get("screen.golemgarage.golem.speed_sight", Math.round(st.speed / GolemStats.BASE_SPEED * 100), Math.round(st.vision)));
        y += 10;
        line(g, sx, y, I18n.get("screen.golemgarage.golem.carry", golem.readUsedSlots(), st.carry) + (st.matchedSet ? "   " + I18n.get("screen.golemgarage.golem.set") : ""));
        y += 14;

        String status = golem.readSyncedStatus();
        boolean stuck = com.xtrip.golemgarage.golem.GolemText.isProblem(status);
        g.drawString(font, I18n.get("screen.golemgarage.golem.now"), sx, y, ACCENT);
        y += 10;
        for (var l : font.split(com.xtrip.golemgarage.golem.GolemText.show(status), 112)) {
            g.drawString(font, l, sx, y, stuck ? 0xFFFF7A6B : TEXT);
            y += 9;
            if (y > y0 + 150) {
                break;
            }
        }
        int active = golem.readActiveRule();
        if (active >= 0 && active < rules.size()) {
            g.drawString(font, font.plainSubstrByWidth(I18n.get("screen.golemgarage.golem.rule_active", active + 1), 112), sx, y, 0xFF8FE39A);
            y += 10;
        }
        List<String> log = golem.readSyncedLog();
        for (int i = log.size() - 1, n = 0; i >= 0 && n < 3 && y < y0 + 178; i--, n++) {
            g.drawString(font, font.plainSubstrByWidth("- " + com.xtrip.golemgarage.golem.GolemText.show(log.get(i)).getString(), 112), sx, y, 0xFF6E7890);
            y += 9;
        }
        // Modules.
        g.drawString(font, I18n.get("screen.golemgarage.golem.modules"), px, y0 + 184, ACCENT);
        var mods = golem.getModules();
        for (int i = 0; i < com.xtrip.golemgarage.golem.GolemModule.SOCKETS; i++) {
            int mx = px + i * 75;
            int my = y0 + 194;
            g.fill(mx, my, mx + 72, my + 20, PANEL);
            if (i < mods.size()) {
                var m = mods.get(i);
                ItemStack icon = new ItemStack(com.xtrip.golemgarage.item.ModItems.MODULES.get(m).get());
                g.renderItem(icon, mx + 2, my + 2);
                g.drawString(font, fit(I18n.get("module.golemgarage." + m.id), 38), mx + 20, my + 6,
                        0xFF000000 | m.color);
            } else {
                g.drawString(font, fit(I18n.get("screen.golemgarage.golem.empty_socket"), 64), mx + 6, my + 6, 0xFF5E6676);
            }
        }
        int linkCount = 0;
        for (GolemLinkerItem.Mode m : GolemLinkerItem.Mode.MARKS) {
            if (golem.readSyncedLink(m) != null) {
                linkCount++;
            }
        }
        int ly = y0 + H - 30 - Math.max(2, linkCount) * 8;
        int lyStart = ly;
        for (GolemLinkerItem.Mode m : GolemLinkerItem.Mode.MARKS) {
            BlockPos p = golem.readSyncedLink(m);
            if (p != null) {
                g.drawString(font, fit(m.title().getString() + " " + p.toShortString(), 112), sx, ly, 0xFF000000 | m.color);
                ly += 8;
            }
        }
        if (ly == lyStart) {
            g.drawString(font, I18n.get("screen.golemgarage.golem.no_links"), sx, ly, 0xFF5E6676);
            g.drawString(font, I18n.get("screen.golemgarage.golem.use_linker"), sx, ly + 9, 0xFF5E6676);
        }
    }

    private String fit(String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, width - font.width("...")) + "...";
    }

    private void bar(GuiGraphics g, int x, int y, String label, float value, float max, int color) {
        g.drawString(font, label + " " + Math.round(value) + "/" + Math.round(max), x, y, TEXT);
        int w = 112;
        int fill = max <= 0 ? 0 : Math.round(w * Math.min(1f, value / max));
        g.fill(x, y + 10, x + w, y + 14, 0xFF0B0E14);
        g.fill(x, y + 10, x + fill, y + 14, color);
    }

    private void line(GuiGraphics g, int x, int y, String text) {
        g.drawString(font, text, x, y, DIM);
    }

    // ================================================================= rules tab
    private RuleEditor editor;

    private RuleEditor editor() {
        if (editor == null) {
            editor = new RuleEditor(rules, () -> {
                dirty = true;
                rebuildWidgets();
            });
        }
        return editor;
    }

    private void initRules(int x0, int y0) {
        editor();
        int group = golem.getGroup();
        if (group > 0) {
            addRenderableWidget(Button.builder(Component.translatable("screen.golemgarage.golem.copy_group", group), b -> {
                PacketDistributor.sendToServer(new SetGolemRulesPayload(golem.getId(), GolemRule.toTag(rules)));
                PacketDistributor.sendToServer(new GolemActionPayload(golem.getId(), GolemActionPayload.COPY_RULES_TO_GROUP, 0));
                dirty = false;
                rebuildWidgets();
            }).bounds(x0 + W - 200, y0 + 6, 108, 16).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("screen.golemgarage.golem.copy_group_tip", group))).build());
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (rulesTab && editor().pickerOpen()) {
            return editor().click(mx, my, button);
        }
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        return rulesTab && editor().click(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (rulesTab && sy != 0) {
            return editor().scroll(mx, my, sy);
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && rulesTab && editor().escape()) {
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ================================================================= frame
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x0 = x0();
        int y0 = y0();
        if (!rulesTab) {
            renderStatus(g, x0, y0, mouseX, mouseY);
        }
        // Widgets on top of the panel contents.
        for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.components.Renderable r) {
                r.render(g, mouseX, mouseY, partialTick);
            }
        }
        if (rulesTab) {
            editor().render(g, font, x0, y0, W, H, mouseX, mouseY, golem.readActiveRule(), dirty);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int x0 = x0();
        int y0 = y0();
        g.fill(x0, y0, x0 + W, y0 + H, BG);
        g.renderOutline(x0, y0, W, H, EDGE);
        g.fill(x0 + 1, y0 + 1, x0 + W - 1, y0 + 2, 0xFF4F6AA0);
    }

    @Override
    public void onClose() {
        if (dirty) {
            PacketDistributor.sendToServer(new SetGolemRulesPayload(golem.getId(), GolemRule.toTag(rules)));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (!golem.isAlive() || minecraft == null || minecraft.player == null
                || golem.distanceToSqr(minecraft.player) > 20 * 20) {
            onClose();
        }
    }
}
