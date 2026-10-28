package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.golem.GolemRule;
import com.xtrip.golemgarage.golem.GolemRule.Action;
import com.xtrip.golemgarage.golem.GolemRule.Trigger;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Rules tab: each rule reads as a sentence ("When power is below 20% -> recharge, unless ..."),
 * with colored parts you click to choose from a grouped, described list. Numbers change with a click
 * (left +, right -) or the scroll wheel. Rules can be moved, deleted, and changes undone.
 */
public class RuleEditor {
    // Colors.
    private static final int CARD = 0xFF222A38;
    private static final int CARD_ACTIVE = 0xFF1F3A2C;
    private static final int CARD_EDGE = 0xFF34405A;
    private static final int WHEN = 0xFF6FB8FF;
    private static final int DO = 0xFF7BE08E;
    private static final int UNLESS = 0xFFFFB45E;
    private static final int NUMBER = 0xFFFFE27A;
    private static final int TEXT = 0xFFE8ECF2;
    private static final int DIM = 0xFF8C96A8;
    private static final int FAINT = 0xFF5E6676;

    private static final int CARD_H = 27;
    private static final int GAP = 3;

    // ---------------------------------------------------------------- wording
    static String phrase(Trigger t) {
        return I18n.get("rule.golemgarage.when." + t.name().toLowerCase(java.util.Locale.ROOT));
    }

    static String phrase(Action a) {
        return I18n.get("rule.golemgarage.do." + a.name().toLowerCase(java.util.Locale.ROOT));
    }

    static String help(Trigger t) {
        return I18n.get("rule.golemgarage.when_help." + t.name().toLowerCase(java.util.Locale.ROOT));
    }

    static String help(Action a) {
        return I18n.get("rule.golemgarage.do_help." + a.name().toLowerCase(java.util.Locale.ROOT));
    }

    // Grouped choices for the picker.
    static final Map<String, List<Trigger>> TRIGGER_GROUPS = new LinkedHashMap<>();
    static final Map<String, List<Action>> ACTION_GROUPS = new LinkedHashMap<>();

    static {
        TRIGGER_GROUPS.put("rule.golemgarage.group.general", List.of(Trigger.ALWAYS));
        TRIGGER_GROUPS.put("rule.golemgarage.group.the_golem", List.of(Trigger.HEALTH_BELOW, Trigger.POWER_BELOW, Trigger.PART_WORN,
                Trigger.PART_BROKEN, Trigger.INVENTORY_FULL, Trigger.INVENTORY_EMPTY));
        TRIGGER_GROUPS.put("rule.golemgarage.group.you", List.of(Trigger.OWNER_WITHIN, Trigger.OWNER_BEYOND, Trigger.OWNER_ATTACKED));
        TRIGGER_GROUPS.put("rule.golemgarage.group.around_it", List.of(Trigger.HOSTILE_WITHIN, Trigger.AWAY_FROM_POST, Trigger.OUTSIDE_AREA,
                Trigger.CROP_READY));
        TRIGGER_GROUPS.put("rule.golemgarage.group.chests", List.of(Trigger.CHEST_A_HAS, Trigger.CHEST_A_BELOW, Trigger.CHEST_B_FULL));
        TRIGGER_GROUPS.put("rule.golemgarage.group.time_and_weather", List.of(Trigger.DAY, Trigger.SUNSET, Trigger.NIGHT, Trigger.RAINING,
                Trigger.THUNDER));
        TRIGGER_GROUPS.put("rule.golemgarage.group.redstone", List.of(Trigger.REDSTONE_ON, Trigger.REDSTONE_OFF));

        ACTION_GROUPS.put("rule.golemgarage.group.moving", List.of(Action.FOLLOW_OWNER, Action.GO_TO_POST, Action.WAIT, Action.STROLL,
                Action.PATROL));
        ACTION_GROUPS.put("rule.golemgarage.group.fighting", List.of(Action.ATTACK_HOSTILE, Action.DEFEND_OWNER));
        ACTION_GROUPS.put("rule.golemgarage.group.items", List.of(Action.TAKE_FROM_A, Action.DEPOSIT_TO_B, Action.TAKE_FROM_NEAREST,
                Action.DEPOSIT_TO_NEAREST, Action.PICK_UP_ITEMS, Action.GIVE_TO_GOLEM));
        ACTION_GROUPS.put("rule.golemgarage.group.farming", List.of(Action.HARVEST_CROPS));
        ACTION_GROUPS.put("rule.golemgarage.group.upkeep", List.of(Action.RECHARGE, Action.REPAIR_SELF, Action.GO_TO_GARAGE,
                Action.GO_HOME));
        ACTION_GROUPS.put("rule.golemgarage.group.redstone", List.of(Action.EMIT_SIGNAL));
    }

    record Preset(String id, ItemStack icon, List<GolemRule> rules) {
        String name() {
            return I18n.get("rule.golemgarage.preset." + id);
        }

        String desc() {
            return I18n.get("rule.golemgarage.preset." + id + ".desc");
        }
    }

    static final List<Preset> PRESETS = List.of(
            new Preset("follower",
                    new ItemStack(Items.LEAD), GolemRule.followerPreset()),
            new Preset("guard",
                    new ItemStack(Items.IRON_SWORD), GolemRule.guardPreset()),
            new Preset("hauler",
                    new ItemStack(Items.CHEST), GolemRule.haulerPreset()),
            new Preset("harvester",
                    new ItemStack(Items.WHEAT), GolemRule.harvesterPreset()),
            new Preset("medic",
                    new ItemStack(Items.GOLDEN_APPLE), GolemRule.medicPreset()),
            new Preset("night_guard",
                    new ItemStack(Items.LANTERN), GolemRule.nightGuardPreset()));

    // ---------------------------------------------------------------- state
    private final List<GolemRule> rules;
    private final Runnable onChange;
    private List<GolemRule> undo;
    private String undoLabel;
    private int scroll;
    /** Open picker: which rule (-1 = a new rule) and which part. */
    private int pickRule = -2;
    private int pickPart;
    private final List<Hot> hots = new ArrayList<>();
    private String tip;

    private record Hot(int x, int y, int w, int h, Runnable left, Runnable right, IntScroll wheel, String tip) {
    }

    private interface IntScroll {
        void by(int dir);
    }

    public RuleEditor(List<GolemRule> rules, Runnable onChange) {
        this.rules = rules;
        this.onChange = onChange;
    }

    public boolean pickerOpen() {
        return pickRule != -2;
    }

    private void remember(String label) {
        undo = new ArrayList<>(rules);
        undoLabel = label;
    }

    private void changed() {
        onChange.run();
    }

    // ---------------------------------------------------------------- render
    public void render(GuiGraphics g, Font font, int x0, int y0, int w, int h, int mx, int my, int active, boolean dirty) {
        hots.clear();
        tip = null;
        boolean picker = pickerOpen();
        int hmx = picker ? -1000 : mx;
        int hmy = picker ? -1000 : my;

        // Presets.
        int px = x0 + 8;
        int py = y0 + 27;
        String qj = I18n.get("rule.golemgarage.ui.quick_jobs");
        g.drawString(font, qj, px, py, DIM);
        g.drawString(font, I18n.get("rule.golemgarage.ui.quick_jobs_hint"), px + font.width(qj) + 6, py, FAINT);
        int cw = (w - 16 - 5 * 4) / 6;
        for (int i = 0; i < PRESETS.size(); i++) {
            Preset p = PRESETS.get(i);
            int cx = px + i * (cw + 4);
            int cy = py + 11;
            boolean hover = in(hmx, hmy, cx, cy, cw, 20);
            g.fill(cx, cy, cx + cw, cy + 20, hover ? 0xFF2E3A50 : CARD);
            g.renderOutline(cx, cy, cw, 20, hover ? 0xFF6FB8FF : CARD_EDGE);
            g.renderItem(p.icon, cx + 2, cy + 2);
            int room = cw - 21;
            int tw = font.width(p.name());
            if (tw <= room) {
                g.drawString(font, p.name(), cx + 19, cy + 6, TEXT);
            } else {
                float sc = (float) room / tw;
                g.pose().pushPose();
                g.pose().translate(cx + 19, cy + 6 + (8 - 8 * sc) / 2f, 0);
                g.pose().scale(sc, sc, 1f);
                g.drawString(font, p.name(), 0, 0, TEXT);
                g.pose().popPose();
            }
            final Preset preset = p;
            hots.add(new Hot(cx, cy, cw, 20, () -> {
                remember(I18n.get("rule.golemgarage.ui.undo_preset", preset.name()));
                rules.clear();
                rules.addAll(preset.rules);
                scroll = 0;
                changed();
            }, null, null, I18n.get("rule.golemgarage.ui.preset_tip", p.name(), p.desc())));
        }

        // Rules list.
        int ly = py + 36;
        String rl = I18n.get("rule.golemgarage.ui.rules");
        g.drawString(font, rl, px, ly, DIM);
        g.drawString(font, font.plainSubstrByWidth(I18n.get("rule.golemgarage.ui.rules_hint"), w - 24 - font.width(rl)), px + font.width(rl) + 6, ly, FAINT);
        int listTop = ly + 11;
        int listBottom = y0 + h - 26;
        int visible = Math.max(1, (listBottom - listTop) / (CARD_H + GAP));
        int maxScroll = Math.max(0, rules.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        g.enableScissor(x0 + 2, listTop - 1, x0 + w - 2, listBottom + 1);
        for (int i = scroll; i < rules.size() && i < scroll + visible; i++) {
            int cy = listTop + (i - scroll) * (CARD_H + GAP);
            renderCard(g, font, i, px, cy, w - 16 - (maxScroll > 0 ? 6 : 0), hmx, hmy, i == active && !dirty);
        }
        if (rules.isEmpty()) {
            g.drawString(font, I18n.get("rule.golemgarage.ui.no_rules"), px, listTop + 6, FAINT);
        }
        g.disableScissor();
        if (maxScroll > 0) {
            int trackH = listBottom - listTop;
            int barH = Math.max(12, trackH * visible / rules.size());
            int barY = listTop + (trackH - barH) * scroll / maxScroll;
            g.fill(x0 + w - 12, listTop, x0 + w - 9, listBottom, 0xFF161B24);
            g.fill(x0 + w - 12, barY, x0 + w - 9, barY + barH, 0xFF4F6AA0);
        }

        // Bottom bar.
        int by = y0 + h - 21;
        if (rules.size() < GolemRule.MAX_RULES) {
            button(g, font, px, by, I18n.get("rule.golemgarage.ui.add"), hmx, hmy, () -> {
                pickRule = -1;
                pickPart = 0;
            }, I18n.get("rule.golemgarage.ui.add_tip"));
        } else {
            g.drawString(font, I18n.get("rule.golemgarage.ui.max", GolemRule.MAX_RULES), px, by + 4, FAINT);
        }
        if (undo != null) {
            button(g, font, px + 82, by, undoLabel, hmx, hmy, () -> {
                rules.clear();
                rules.addAll(undo);
                undo = null;
                changed();
            }, I18n.get("rule.golemgarage.ui.undo_tip"));
        }
        String note = dirty ? I18n.get("rule.golemgarage.ui.saves_on_close") : (active >= 0 ? I18n.get("rule.golemgarage.ui.green_running") : "");
        g.drawString(font, note, x0 + w - 8 - font.width(note), by + 4, dirty ? 0xFFFFE27A : FAINT);

        if (picker) {
            g.flush();
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            renderPicker(g, font, x0, y0, w, h, mx, my);
            g.pose().popPose();
        } else {
            hoverTip(mx, my);
            if (tip != null) {
                List<FormattedCharSequence> lines = new ArrayList<>();
                for (String part : tip.split("\n")) {
                    lines.addAll(font.split(Component.literal(part), 200));
                }
                g.renderTooltip(font, lines, mx, my);
            }
        }
    }

    private void renderCard(GuiGraphics g, Font font, int i, int x, int y, int w, int mx, int my, boolean running) {
        GolemRule r = rules.get(i);
        g.fill(x, y, x + w, y + CARD_H, running ? CARD_ACTIVE : CARD);
        g.renderOutline(x, y, w, CARD_H, running ? 0xFF36C06A : CARD_EDGE);
        // Number badge.
        g.fill(x + 3, y + 3, x + 15, y + CARD_H - 3, running ? 0xFF36C06A : 0xFF34405A);
        String n = String.valueOf(i + 1);
        g.drawString(font, n, x + 9 - font.width(n) / 2, y + 10, TEXT);

        int tx = x + 20;
        // Line 1: When ... unless ...
        int l1 = y + 4;
        boolean always = r.trigger() == Trigger.ALWAYS;
        String lead = always ? "" : I18n.get("rule.golemgarage.ui.when");
        g.drawString(font, lead, tx, l1, DIM);
        int cx = tx + font.width(lead);
        final int idx = i;
        cx = chip(g, font, cx, l1, always ? I18n.get("rule.golemgarage.ui.always") : phrase(r.trigger()), r.trigger().hasParam, r.param(), WHEN, mx, my,
                () -> openPicker(idx, 0),
                d -> setParam(idx, 0, d), help(r.trigger()));
        if (r.unless() != Trigger.NONE) {
            String unl = I18n.get("rule.golemgarage.ui.unless");
            g.drawString(font, unl, cx, l1, DIM);
            cx += font.width(unl);
            chip(g, font, cx, l1, phrase(r.unless()), r.unless().hasParam, r.unlessParam(), UNLESS, mx, my,
                    () -> openPicker(idx, 2), d -> setParam(idx, 2, d), I18n.get("rule.golemgarage.ui.exception", help(r.unless())));
        } else {
            int ux = cx + 8;
            String add = I18n.get("rule.golemgarage.ui.add_unless");
            boolean hover = in(mx, my, ux, l1 - 1, font.width(add), 10);
            g.drawString(font, add, ux, l1, hover ? UNLESS : FAINT);
            hots.add(new Hot(ux, l1 - 1, font.width(add), 10, () -> openPicker(idx, 2), null, null,
                    I18n.get("rule.golemgarage.ui.add_unless_tip")));
        }
        // Line 2: -> action
        int l2 = y + 16;
        g.drawString(font, "→", tx + 2, l2, DO);
        chip(g, font, tx + 12, l2, phrase(r.action()), r.action().hasParam, r.actionParam(), DO, mx, my,
                () -> openPicker(idx, 1), d -> setParam(idx, 1, d), help(r.action()));

        // Controls: up, down, delete.
        int bx = x + w - 40;
        icon(g, font, bx, y + 8, "▲", i > 0, mx, my, () -> move(idx, -1), I18n.get("rule.golemgarage.ui.up"));
        icon(g, font, bx + 13, y + 8, "▼", i < rules.size() - 1, mx, my, () -> move(idx, 1), I18n.get("rule.golemgarage.ui.down"));
        icon(g, font, bx + 26, y + 8, "✖", true, mx, my, () -> {
            remember(I18n.get("rule.golemgarage.ui.undo_delete"));
            rules.remove(idx);
            changed();
        }, I18n.get("rule.golemgarage.ui.delete"));
    }

    /** Draws a clickable phrase; a number in it is its own chip you can scroll or click. Returns the end x. */
    private int chip(GuiGraphics g, Font font, int x, int y, String text, boolean hasNum, int value, int color,
                     int mx, int my, Runnable open, IntScroll num, String help) {
        String before = text;
        String after = "";
        String number = "";
        int at = text.indexOf("{}");
        if (at >= 0) {
            before = text.substring(0, at);
            after = text.substring(at + 2);
            number = String.valueOf(value);
        }
        int w = font.width(before) + font.width(number) + font.width(after);
        boolean hover = in(mx, my, x - 2, y - 2, w + 4, 11);
        g.fill(x - 2, y - 2, x + w + 2, y + 9, hover ? (color & 0x00FFFFFF) | 0x50000000 : (color & 0x00FFFFFF) | 0x26000000);
        g.fill(x - 2, y + 9, x + w + 2, y + 10, color);
        int cx = x;
        g.drawString(font, before, cx, y, color);
        cx += font.width(before);
        if (!number.isEmpty()) {
            int nw = font.width(number);
            boolean nh = in(mx, my, cx - 1, y - 2, nw + 2, 11);
            g.fill(cx - 1, y - 2, cx + nw + 1, y + 9, nh ? 0x90FFE27A : 0x40FFE27A);
            g.drawString(font, number, cx, y, nh ? 0xFF1B1B1B : NUMBER);
            hots.add(new Hot(cx - 1, y - 2, nw + 2, 11, () -> num.by(1), () -> num.by(-1), num::by,
                    I18n.get("rule.golemgarage.ui.number_tip")));
            cx += nw;
        }
        g.drawString(font, after, cx, y, color);
        hots.add(new Hot(x - 2, y - 2, w + 4, 11, open, null, null, help + "\n" + I18n.get("rule.golemgarage.ui.click_change")));
        return x + w;
    }

    private void icon(GuiGraphics g, Font font, int x, int y, String s, boolean enabled, int mx, int my, Runnable act, String help) {
        boolean hover = enabled && in(mx, my, x - 1, y - 1, 11, 11);
        g.fill(x - 1, y - 1, x + 10, y + 10, hover ? 0xFF3A4A66 : 0xFF2A3344);
        int col = enabled ? (hover ? TEXT : DIM) : 0xFF404858;
        if (s.equals("\u25B2") || s.equals("\u25BC")) {
            boolean up = s.equals("\u25B2");
            for (int i = 0; i < 4; i++) {
                int yy = up ? y + 3 + i : y + 6 - i;
                g.fill(x + 4 - i, yy, x + 5 + i, yy + 1, col);
            }
        } else {
            for (int i = 0; i < 6; i++) {
                g.fill(x + 2 + i, y + 2 + i, x + 3 + i, y + 3 + i, hover ? 0xFFFF7A6B : col);
                g.fill(x + 7 - i, y + 2 + i, x + 8 - i, y + 3 + i, hover ? 0xFFFF7A6B : col);
            }
        }
        if (enabled) {
            hots.add(new Hot(x - 1, y - 1, 11, 11, act, null, null, help));
        }
    }

    private void button(GuiGraphics g, Font font, int x, int y, String label, int mx, int my, Runnable act, String help) {
        int w = font.width(label) + 10;
        boolean hover = in(mx, my, x, y, w, 15);
        g.fill(x, y, x + w, y + 15, hover ? 0xFF35507A : 0xFF2A3A55);
        g.renderOutline(x, y, w, 15, hover ? 0xFF6FB8FF : 0xFF3F5478);
        g.drawString(font, label, x + 5, y + 4, TEXT);
        hots.add(new Hot(x, y, w, 15, act, null, null, help));
    }

    // ---------------------------------------------------------------- picker
    private void openPicker(int rule, int part) {
        pickRule = rule;
        pickPart = part;
    }

    private record Choice(int x, int y, int w, Enum<?> value) {
    }

    private final List<Choice> choices = new ArrayList<>();

    private void renderPicker(GuiGraphics g, Font font, int x0, int y0, int w, int h, int mx, int my) {
        choices.clear();
        g.fill(x0, y0, x0 + w, y0 + h, 0xB0000000);
        int pw = w - 30;
        int ph = h - 24;
        int px = x0 + 15;
        int py = y0 + 12;
        g.fill(px, py, px + pw, py + ph, 0xFF161B24);
        int accent = pickPart == 0 ? WHEN : pickPart == 1 ? DO : UNLESS;
        g.renderOutline(px, py, pw, ph, accent);
        String title = switch (pickPart) {
            case 0 -> pickRule == -1 ? I18n.get("rule.golemgarage.ui.pick_when_new") : I18n.get("rule.golemgarage.ui.pick_when");
            case 1 -> pickRule == -1 ? I18n.get("rule.golemgarage.ui.pick_do_new") : I18n.get("rule.golemgarage.ui.pick_do");
            default -> I18n.get("rule.golemgarage.ui.pick_unless");
        };
        g.drawString(font, title, px + 8, py + 6, accent);
        String esc = I18n.get("rule.golemgarage.ui.esc");
        g.drawString(font, esc, px + pw - 8 - font.width(esc), py + 6, FAINT);

        Map<String, ? extends List<? extends Enum<?>>> groups = pickPart == 1 ? ACTION_GROUPS : TRIGGER_GROUPS;
        int colW = (pw - 24) / 2;
        int[] colY = {py + 20, py + 20};
        int col = 0;
        String hoverHelp = null;
        if (pickPart == 2) {
            hoverHelp = drawChoice(g, font, px + 8, colY[0], colW, Trigger.NONE, I18n.get("rule.golemgarage.ui.no_exception"), accent, mx, my, hoverHelp);
            colY[0] += 12;
        }
        for (Map.Entry<String, ? extends List<? extends Enum<?>>> e : groups.entrySet()) {
            List<? extends Enum<?>> items = e.getValue();
            if (pickPart == 2 && items.contains(Trigger.ALWAYS)) {
                continue;
            }
            int need = 11 + items.size() * 11;
            col = colY[0] <= colY[1] ? 0 : 1;
            int cx = px + 8 + col * (colW + 8);
            g.drawString(font, I18n.get(e.getKey()), cx, colY[col], FAINT);
            colY[col] += 11;
            for (Enum<?> v : items) {
                String label = v instanceof Trigger t ? (t == Trigger.ALWAYS ? I18n.get("rule.golemgarage.ui.always") : cap(phrase(t)))
                        : cap(phrase((Action) v));
                label = label.replace("{}", "N");
                if (v instanceof Action act && act.isDisabled()) {
                    label += " " + I18n.get("rule.golemgarage.ui.off");
                }
                hoverHelp = drawChoice(g, font, cx, colY[col], colW, v, label, accent, mx, my, hoverHelp);
                colY[col] += 11;
            }
            colY[col] += 3;
        }
        if (hoverHelp != null) {
            int hy = py + ph - 22;
            g.fill(px + 1, hy - 3, px + pw - 1, py + ph - 1, 0xFF1E2430);
            int yy = hy;
            for (FormattedCharSequence l : font.split(Component.literal(hoverHelp), pw - 16)) {
                g.drawString(font, l, px + 8, yy, TEXT);
                yy += 9;
            }
        }
    }

    private String drawChoice(GuiGraphics g, Font font, int x, int y, int w, Enum<?> v, String label, int accent,
                              int mx, int my, String hoverHelp) {
        boolean current = isCurrent(v);
        boolean hover = in(mx, my, x - 2, y - 1, w, 11);
        if (hover || current) {
            g.fill(x - 2, y - 1, x + w - 2, y + 10, hover ? (accent & 0x00FFFFFF) | 0x55000000 : 0x30FFFFFF);
        }
        g.drawString(font, font.plainSubstrByWidth(label, w - 4), x, y + 1, current ? accent : TEXT);
        choices.add(new Choice(x - 2, y - 1, w, v));
        if (hover) {
            return v instanceof Trigger t ? help(t) : help((Action) v);
        }
        return hoverHelp;
    }

    private boolean isCurrent(Enum<?> v) {
        if (pickRule < 0) {
            return false;
        }
        GolemRule r = rules.get(pickRule);
        return switch (pickPart) {
            case 0 -> r.trigger() == v;
            case 1 -> r.action() == v;
            default -> r.unless() == v;
        };
    }

    private GolemRule pendingNew;

    private void choose(Enum<?> v) {
        if (pickRule == -1) {
            if (pickPart == 0) {
                Trigger t = (Trigger) v;
                pendingNew = new GolemRule(t, t.defaultParam, Action.FOLLOW_OWNER);
                pickPart = 1;
                return;
            }
            Action a = (Action) v;
            remember(I18n.get("rule.golemgarage.ui.undo_add"));
            rules.add(new GolemRule(pendingNew.trigger(), pendingNew.param(), a, a.defaultParam, Trigger.NONE, 0));
            scroll = rules.size();
            pickRule = -2;
            changed();
            return;
        }
        GolemRule r = rules.get(pickRule);
        remember(I18n.get("rule.golemgarage.ui.undo_change"));
        GolemRule nr = switch (pickPart) {
            case 0 -> {
                Trigger t = (Trigger) v;
                yield new GolemRule(t, t.defaultParam, r.action(), r.actionParam(), r.unless(), r.unlessParam());
            }
            case 1 -> {
                Action a = (Action) v;
                yield new GolemRule(r.trigger(), r.param(), a, a.defaultParam, r.unless(), r.unlessParam());
            }
            default -> {
                Trigger u = (Trigger) v;
                yield new GolemRule(r.trigger(), r.param(), r.action(), r.actionParam(), u, u.defaultParam);
            }
        };
        rules.set(pickRule, nr);
        pickRule = -2;
        changed();
    }

    // ---------------------------------------------------------------- editing
    private void setParam(int idx, int part, int dir) {
        GolemRule r = rules.get(idx);
        GolemRule nr = switch (part) {
            case 0 -> new GolemRule(r.trigger(), clamp(r.param() + dir * r.trigger().step, r.trigger().min, r.trigger().max),
                    r.action(), r.actionParam(), r.unless(), r.unlessParam());
            case 1 -> new GolemRule(r.trigger(), r.param(), r.action(),
                    clamp(r.actionParam() + dir * r.action().step, r.action().min, r.action().max), r.unless(), r.unlessParam());
            default -> new GolemRule(r.trigger(), r.param(), r.action(), r.actionParam(), r.unless(),
                    clamp(r.unlessParam() + dir * r.unless().step, r.unless().min, r.unless().max));
        };
        if (!nr.equals(r)) {
            rules.set(idx, nr);
            changed();
        }
    }

    private void move(int idx, int dir) {
        int to = idx + dir;
        if (to < 0 || to >= rules.size()) {
            return;
        }
        GolemRule r = rules.remove(idx);
        rules.add(to, r);
        changed();
    }

    // ---------------------------------------------------------------- input
    public boolean click(double mx, double my, int button) {
        if (pickerOpen()) {
            for (Choice c : choices) {
                if (in(mx, my, c.x, c.y, c.w, 11)) {
                    choose(c.value);
                    return true;
                }
            }
            return true; // swallow clicks while the picker is open
        }
        for (int i = hots.size() - 1; i >= 0; i--) {
            Hot h = hots.get(i);
            if (in(mx, my, h.x, h.y, h.w, h.h)) {
                if (button == 1 && h.right != null) {
                    h.right.run();
                    return true;
                }
                if (button == 0 && h.left != null) {
                    h.left.run();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean scroll(double mx, double my, double dy) {
        if (pickerOpen()) {
            return true;
        }
        for (int i = hots.size() - 1; i >= 0; i--) {
            Hot h = hots.get(i);
            if (h.wheel != null && in(mx, my, h.x, h.y, h.w, h.h)) {
                h.wheel.by(dy > 0 ? 1 : -1);
                return true;
            }
        }
        scroll += dy > 0 ? -1 : 1;
        return true;
    }

    /** Escape closes the picker first. Returns true if it was handled. */
    public boolean escape() {
        if (pickerOpen()) {
            pickRule = -2;
            return true;
        }
        return false;
    }

    public void hoverTip(double mx, double my) {
        for (int i = hots.size() - 1; i >= 0; i--) {
            Hot h = hots.get(i);
            if (h.tip != null && in(mx, my, h.x, h.y, h.w, h.h)) {
                tip = h.tip;
                return;
            }
        }
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
