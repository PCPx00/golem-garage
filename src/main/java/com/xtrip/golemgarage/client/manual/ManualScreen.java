package com.xtrip.golemgarage.client.manual;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.entity.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The Golem Engineer's Manual: a two-page book with chapters, entries, live golem previews and recipes. */
public class ManualScreen extends Screen {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "textures/gui/manual.png");
    static final int TEX_W = 512;
    static final int TEX_H = 256;
    static final int BOOK_W = 320;
    static final int BOOK_H = 200;
    static final int PAGE_W = 128;
    static final int PAGE_H = 160;
    static final int LEFT_X = 20;
    static final int RIGHT_X = 172;
    static final int PAGE_Y = 18;

    enum View { HOME, CATEGORY, ENTRY }

    record State(View view, String id, int spread) {
    }

    /** Where the reader was; the book reopens there. */
    private static State lastState = new State(View.HOME, null, 0);
    private static String lastSearch = "";

    private ManualBook book;
    private View view = View.HOME;
    private ManualBook.Category category;
    private ManualBook.Entry entry;
    private int spread;
    private final Deque<State> history = new ArrayDeque<>();
    private final List<Hot> hots = new ArrayList<>();
    private final Map<String, GolemEntity> golems = new HashMap<>();
    private EditBox search;
    private List<CategoryPage> categoryPages = List.of();

    record Hot(int x, int y, int w, int h, ItemStack stack, String entry, String category) {
    }

    /** Context handed to pages while drawing. */
    public final class Ctx {
        public final GuiGraphics g;
        public final net.minecraft.client.gui.Font font;
        public final int mouseX;
        public final int mouseY;
        public final long time;
        public final ManualScreen screen = ManualScreen.this;

        Ctx(GuiGraphics g, int mouseX, int mouseY) {
            this.g = g;
            this.font = ManualScreen.this.font;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.time = net.minecraft.Util.getMillis();
        }

        public void hot(int x, int y, int w, int h, ItemStack stack, String link) {
            hots.add(new Hot(x, y, w, h, stack, link, null));
        }

        public HolderLookup.Provider registries() {
            return Minecraft.getInstance().level.registryAccess();
        }
    }

    public ManualScreen() {
        super(Component.translatable("item.golemgarage.manual"));
    }

    private int bx() {
        return (width - BOOK_W) / 2;
    }

    private int by() {
        return (height - BOOK_H) / 2;
    }

    @Override
    protected void init() {
        if (book == null) {
            book = ManualBook.load().orElse(null);
            if (book != null) {
                restore(lastState);
                if (com.xtrip.golemgarage.entity.GolemEntity.DEBUG) {
                    checkLayout();
                }
            }
        }
        search = new EditBox(font, bx() + LEFT_X + 4, by() + PAGE_Y + 140, PAGE_W - 8, 10, Component.translatable("screen.golemgarage.manual.search"));
        search.setBordered(false);
        search.setTextColor(0x3A3226);
        search.setHint(Component.translatable("screen.golemgarage.manual.search_hint").withStyle(s -> s.withColor(0x8C8170)));
        search.setValue(lastSearch);
        search.setResponder(v -> lastSearch = v);
        search.setMaxLength(40);
        search.visible = view == View.HOME;
        addWidget(search);
    }

    private State state() {
        String id = switch (view) {
            case HOME -> null;
            case CATEGORY -> category.id;
            case ENTRY -> entry.id;
        };
        return new State(view, id, spread);
    }

    private void restore(State s) {
        view = View.HOME;
        spread = 0;
        if (s.view == View.CATEGORY) {
            for (ManualBook.Category c : book.categories) {
                if (c.id.equals(s.id)) {
                    category = c;
                    view = View.CATEGORY;
                }
            }
        } else if (s.view == View.ENTRY && book.entries.containsKey(s.id)) {
            entry = book.entries.get(s.id);
            category = entry.category;
            view = View.ENTRY;
        }
        if (view != View.HOME) {
            spread = Mth.clamp(s.spread, 0, Math.max(0, spreadCount() - 1));
        }
        if (search != null) {
            search.visible = view == View.HOME;
            if (view != View.HOME) {
                search.setFocused(false);
            }
        }
    }

    private void go(State s) {
        history.push(state());
        restore(s);
        pageSound();
    }

    private void back() {
        if (!history.isEmpty()) {
            restore(history.pop());
            pageSound();
        } else if (view == View.ENTRY) {
            restore(new State(View.CATEGORY, category.id, 0));
            pageSound();
        } else if (view == View.CATEGORY) {
            restore(new State(View.HOME, null, 0));
            pageSound();
        }
    }

    private void pageSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    @Override
    public void removed() {
        if (book != null) {
            lastState = state();
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Debug runs: lists every page whose text spilled onto an extra page. */
    private void checkLayout() {
        for (ManualBook.Entry e : book.entries.values()) {
            int spills = 0;
            for (int i = 0; i < e.pages.size(); i++) {
                var o = e.pages.get(i).deepCopy();
                if (i == 0 && !o.has("title")) {
                    o.addProperty("title", e.title);
                }
                int n = ManualPage.build(o, font, PAGE_W, PAGE_H).size();
                if (n > 1 && !"table".equals(ManualBook.str(o, "type", "text"))) {
                    spills++;
                    GolemGarage.LOGGER.info("[manual] {} page {} spills onto {} pages", e.id, i + 1, n);
                }
            }
        }
        GolemGarage.LOGGER.info("[manual] layout check done: {} entries", book.entries.size());
    }

    // =================================================================== layout
    private List<ManualPage> entryPages() {
        if (entry.laidOut == null) {
            List<ManualPage> out = new ArrayList<>();
            for (int i = 0; i < entry.pages.size(); i++) {
                var o = entry.pages.get(i).deepCopy();
                if (i == 0 && !o.has("title")) {
                    o.addProperty("title", entry.title);
                }
                out.addAll(ManualPage.build(o, font, PAGE_W, PAGE_H));
            }
            entry.laidOut = out;
        }
        return entry.laidOut;
    }

    /** One page of a category's entry list. */
    record CategoryPage(boolean header, List<FormattedCharSequence> desc, List<ManualBook.Entry> rows) {
    }

    private static final int ROW_H = 15;

    private List<CategoryPage> categoryPages() {
        List<CategoryPage> pages = new ArrayList<>();
        List<FormattedCharSequence> desc = font.split(Component.literal(category.desc), PAGE_W);
        int headerH = ManualPage.TITLE_H + desc.size() * ManualPage.LINE_H + 6;
        int i = 0;
        boolean first = true;
        while (i < category.entries.size() || first) {
            int avail = PAGE_H - (first ? headerH : 0);
            int n = Math.max(0, avail / ROW_H);
            int end = Math.min(category.entries.size(), i + n);
            pages.add(new CategoryPage(first, first ? desc : List.of(), category.entries.subList(i, end)));
            i = end;
            first = false;
        }
        return pages;
    }

    private int spreadCount() {
        return switch (view) {
            case HOME -> 1;
            case CATEGORY -> (categoryPages().size() + 1) / 2;
            case ENTRY -> (entryPages().size() + 1) / 2;
        };
    }

    // =================================================================== rendering
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        hots.clear();
        int x0 = bx();
        int y0 = by();
        g.blit(TEX, x0, y0, 0, 0, BOOK_W, BOOK_H, TEX_W, TEX_H);
        if (book == null) {
            g.drawCenteredString(font, Component.translatable("screen.golemgarage.manual.missing"), x0 + BOOK_W / 2, y0 + 90, 0xFFFF6060);
            return;
        }
        Ctx ctx = new Ctx(g, mouseX, mouseY);
        switch (view) {
            case HOME -> renderHome(ctx, x0, y0);
            case CATEGORY -> renderCategory(ctx, x0, y0);
            case ENTRY -> renderEntry(ctx, x0, y0);
        }
        renderNav(ctx, x0, y0);
        if (view == View.HOME) {
            drawFrame(g, x0 + LEFT_X, y0 + PAGE_Y + 136, PAGE_W, 16);
            search.render(g, mouseX, mouseY, partial);
        }
        renderTooltip(ctx);
    }

    private void renderHome(Ctx ctx, int x0, int y0) {
        int lx = x0 + LEFT_X;
        int ly = y0 + PAGE_Y;
        GuiGraphics g = ctx.g;
        int y = ly;
        for (FormattedCharSequence l : font.split(Component.literal(book.title).withStyle(s -> s.withBold(true)), PAGE_W)) {
            g.drawString(font, l, lx + (PAGE_W - font.width(l)) / 2, y, ManualPage.TITLE, false);
            y += 10;
        }
        drawDivider(g, lx + PAGE_W / 2, y + 1);
        y += 6;
        for (FormattedCharSequence l : font.split(Component.literal(book.subtitle), PAGE_W)) {
            g.drawString(font, l, lx + (PAGE_W - font.width(l)) / 2, y, ManualPage.MUTED, false);
            y += 9;
        }
        int golemTop = y + 2;
        int golemBottom = y0 + PAGE_Y + 130;
        drawPedestal(g, lx + PAGE_W / 2, golemBottom);
        renderGolem(g, HOME_GOLEMS[(int) ((ctx.time / 4000) % HOME_GOLEMS.length)], lx + PAGE_W / 2, golemBottom - 1,
                (golemBottom - golemTop) * 0.33f, lx, golemTop, PAGE_W, golemBottom - golemTop + 4);

        // Right page: chapters, or search results.
        int rx = x0 + RIGHT_X;
        int ry = y0 + PAGE_Y;
        String q = search.getValue().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            ManualPage.drawTitle(ctx, I18n.get("screen.golemgarage.manual.chapters"), rx, ry, PAGE_W);
            int rows = book.categories.size();
            int rowH = Math.min(ROW_H, (PAGE_H - ManualPage.TITLE_H) / Math.max(1, rows));
            int yy = ry + ManualPage.TITLE_H;
            for (ManualBook.Category c : book.categories) {
                row(ctx, rx, yy, rowH, c.icon, c.title, null, c.id);
                yy += rowH;
            }
        } else {
            List<ManualBook.Entry> found = new ArrayList<>();
            for (ManualBook.Entry e : book.entries.values()) {
                if (e.title.toLowerCase(Locale.ROOT).contains(q) || e.category.title.toLowerCase(Locale.ROOT).contains(q)) {
                    found.add(e);
                }
            }
            ManualPage.drawTitle(ctx, found.isEmpty() ? I18n.get("screen.golemgarage.manual.nothing_found") : I18n.get("screen.golemgarage.manual.found", found.size()), rx, ry, PAGE_W);
            int yy = ry + ManualPage.TITLE_H;
            int max = (PAGE_H - ManualPage.TITLE_H) / ROW_H;
            for (int i = 0; i < found.size() && i < max; i++) {
                ManualBook.Entry e = found.get(i);
                row(ctx, rx, yy, ROW_H, e.icon, e.title, e.id, null);
                yy += ROW_H;
            }
        }
    }

    private static final String[] HOME_GOLEMS = {
            "{Parts:{head:{Shape:\"sentry_head\",Material:\"iron\"},torso:{Shape:\"plated_chest\",Material:\"gold\"},arms:{Shape:\"golem_fists\",Material:\"copper\"},legs:{Shape:\"strider_legs\",Material:\"netherite\"},core:{Shape:\"soul_core\",Material:\"steel\"}}}",
            "{Parts:{head:{Shape:\"scope_head\",Material:\"diamond\"},torso:{Shape:\"cargo_barrel\",Material:\"diamond\"},arms:{Shape:\"grabber_claws\",Material:\"diamond\"},legs:{Shape:\"spider_legs\",Material:\"diamond\"},core:{Shape:\"lightning_core\",Material:\"diamond\"}}}",
            "{Parts:{head:{Shape:\"lantern_head\",Material:\"gold\"},torso:{Shape:\"furnace_chest\",Material:\"gold\"},arms:{Shape:\"hammer_arms\",Material:\"gold\"},legs:{Shape:\"tread_legs\",Material:\"gold\"},core:{Shape:\"furnace_core\",Material:\"gold\"}}}",
            "{Parts:{head:{Shape:\"visor_head\",Material:\"netherite\"},torso:{Shape:\"sealed_hold\",Material:\"steel\"},arms:{Shape:\"scythe_arms\",Material:\"netherite\"},legs:{Shape:\"wheel_legs\",Material:\"steel\"},core:{Shape:\"solar_core\",Material:\"netherite\"}}}",
    };

    /** A clickable list row with an icon. */
    private void row(Ctx ctx, int x, int y, int h, ItemStack icon, String label, String entryId, String categoryId) {
        boolean hover = ctx.mouseX >= x && ctx.mouseX < x + PAGE_W && ctx.mouseY >= y && ctx.mouseY < y + h;
        if (hover) {
            ctx.g.fill(x - 2, y, x + PAGE_W + 1, y + h, 0x30406080);
        }
        var pose = ctx.g.pose();
        float s = Math.min(1f, (h - 1) / 16f);
        pose.pushPose();
        pose.translate(x, y + (h - 16 * s) / 2f, 0);
        pose.scale(s, s, 1f);
        ctx.g.renderItem(icon, 0, 0);
        pose.popPose();
        int tx = x + Math.round(16 * s) + 4;
        String text = font.plainSubstrByWidth(label, x + PAGE_W - tx);
        ctx.g.drawString(font, text, tx, y + (h - 8) / 2, hover ? ManualPage.TITLE : ManualPage.TEXT, false);
        hots.add(new Hot(x - 2, y, PAGE_W + 3, h, null, entryId, categoryId));
    }

    private void renderCategory(Ctx ctx, int x0, int y0) {
        categoryPages = categoryPages();
        for (int side = 0; side < 2; side++) {
            int idx = spread * 2 + side;
            if (idx >= categoryPages.size()) {
                break;
            }
            CategoryPage p = categoryPages.get(idx);
            int x = x0 + (side == 0 ? LEFT_X : RIGHT_X);
            int y = y0 + PAGE_Y;
            if (p.header) {
                ManualPage.drawTitle(ctx, category.title, x, y, PAGE_W);
                y += ManualPage.TITLE_H;
                for (FormattedCharSequence l : p.desc) {
                    ctx.g.drawString(font, l, x, y, ManualPage.TEXT, false);
                    y += ManualPage.LINE_H;
                }
                y += 6;
            }
            for (ManualBook.Entry e : p.rows) {
                row(ctx, x, y, ROW_H, e.icon, e.title, e.id, null);
                y += ROW_H;
            }
            pageNumber(ctx, x0, side, idx + 1, categoryPages.size());
        }
    }

    private void renderEntry(Ctx ctx, int x0, int y0) {
        List<ManualPage> pages = entryPages();
        for (int side = 0; side < 2; side++) {
            int idx = spread * 2 + side;
            if (idx >= pages.size()) {
                break;
            }
            int x = x0 + (side == 0 ? LEFT_X : RIGHT_X);
            pages.get(idx).render(ctx, x, y0 + PAGE_Y, PAGE_W, PAGE_H);
            pageNumber(ctx, x0, side, idx + 1, pages.size());
        }
        // Breadcrumb over the book.
        String crumb = category.title + "  >  " + entry.title;
        ctx.g.drawString(font, font.plainSubstrByWidth(crumb, BOOK_W - 20), x0 + 10, y0 - 11, 0xFFE8DCC0, true);
    }

    private void pageNumber(Ctx ctx, int x0, int side, int n, int total) {
        String s = n + " / " + total;
        int cx = x0 + (side == 0 ? LEFT_X : RIGHT_X) + PAGE_W / 2;
        ctx.g.drawString(font, s, cx - font.width(s) / 2, by() + 183, ManualPage.MUTED, false);
    }

    // Navigation buttons: texture positions.
    private static final int NEXT_U = 48;
    private static final int PREV_U = 68;
    private static final int BACK_U = 88;
    private static final int HOME_U = 104;
    private static final int BTN_V = 208;

    private int[] nextBtn() {
        return new int[]{bx() + BOOK_W - 34, by() + 182, 18, 10};
    }

    private int[] prevBtn() {
        return new int[]{bx() + 16, by() + 182, 18, 10};
    }

    private int[] backBtn() {
        return new int[]{bx() + BOOK_W / 2 - 6, by() + 183, 13, 10};
    }

    private int[] homeBtn() {
        return new int[]{bx() + BOOK_W / 2 - 6, by() + 5, 13, 11};
    }

    private boolean over(int[] b, double mx, double my) {
        return mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3];
    }

    private void renderNav(Ctx ctx, int x0, int y0) {
        if (view == View.HOME) {
            return;
        }
        int[] home = homeBtn();
        button(ctx, home, HOME_U, 11);
        int[] backB = backBtn();
        button(ctx, backB, BACK_U, 10);
        if (spread > 0) {
            button(ctx, prevBtn(), PREV_U, 10);
        }
        if (spread < spreadCount() - 1) {
            button(ctx, nextBtn(), NEXT_U, 10);
        }
    }

    private void button(Ctx ctx, int[] b, int u, int h) {
        boolean hover = over(b, ctx.mouseX, ctx.mouseY);
        ctx.g.blit(TEX, b[0], b[1], u, BTN_V + (hover ? h + 1 : 0), b[2], b[3], TEX_W, TEX_H);
    }

    private void renderTooltip(Ctx ctx) {
        for (Hot h : hots) {
            if (ctx.mouseX >= h.x && ctx.mouseX < h.x + h.w && ctx.mouseY >= h.y && ctx.mouseY < h.y + h.h) {
                if (h.stack == null || h.stack.isEmpty()) {
                    return;
                }
                List<Component> lines = new ArrayList<>(getTooltipFromItem(Minecraft.getInstance(), h.stack));
                ManualBook.Entry target = linkFor(h);
                if (target != null && target != entry) {
                    lines.add(Component.translatable("screen.golemgarage.manual.click_read", target.title).withStyle(s -> s.withColor(0x7FB8FF)));
                }
                ctx.g.renderComponentTooltip(font, lines, ctx.mouseX, ctx.mouseY);
                return;
            }
        }
    }

    private ManualBook.Entry linkFor(Hot h) {
        if (h.entry != null) {
            return book.entries.get(h.entry);
        }
        return h.stack == null ? null : book.entryFor(h.stack);
    }

    // =================================================================== input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (book == null) {
            return false;
        }
        if (button == 1 && view != View.HOME) {
            back();
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (view != View.HOME) {
            if (over(homeBtn(), mx, my)) {
                go(new State(View.HOME, null, 0));
                return true;
            }
            if (over(backBtn(), mx, my)) {
                back();
                return true;
            }
            if (spread > 0 && over(prevBtn(), mx, my)) {
                turn(-1);
                return true;
            }
            if (spread < spreadCount() - 1 && over(nextBtn(), mx, my)) {
                turn(1);
                return true;
            }
        }
        for (Hot h : new ArrayList<>(hots)) {
            if (mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                if (h.category != null) {
                    go(new State(View.CATEGORY, h.category, 0));
                    return true;
                }
                ManualBook.Entry target = linkFor(h);
                if (target != null && target != entry) {
                    go(new State(View.ENTRY, target.id, 0));
                    return true;
                }
            }
        }
        return false;
    }

    private void turn(int dir) {
        int next = Mth.clamp(spread + dir, 0, spreadCount() - 1);
        if (next != spread) {
            spread = next;
            pageSound();
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (view != View.HOME && sy != 0) {
            turn(sy < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (search != null && search.isFocused() && key != InputConstants.KEY_ESCAPE) {
            return search.keyPressed(key, scan, mods) || search.canConsumeInput();
        }
        if (view != View.HOME) {
            if (key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_PAGEDOWN) {
                turn(1);
                return true;
            }
            if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_PAGEUP) {
                turn(-1);
                return true;
            }
            if (key == InputConstants.KEY_BACKSPACE) {
                back();
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    // =================================================================== drawing helpers
    void drawDivider(GuiGraphics g, int cx, int y) {
        g.blit(TEX, cx - 40, y, 0, 230, 80, 5, TEX_W, TEX_H);
    }

    void drawSlot(GuiGraphics g, int x, int y) {
        g.blit(TEX, x, y, 0, 208, 18, 18, TEX_W, TEX_H);
    }

    void drawArrow(GuiGraphics g, int x, int y) {
        g.blit(TEX, x, y, 24, 208, 22, 15, TEX_W, TEX_H);
    }

    void drawPedestal(GuiGraphics g, int cx, int y) {
        g.blit(TEX, cx - 32, y - 4, 120, 208, 64, 10, TEX_W, TEX_H);
    }

    void drawFrame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF8C7A5B);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFD9CBA8);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF6E5F45);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFF6E5F45);
    }

    /** Renders a slowly turning golem built from SNBT data, clipped to the given box. */
    void renderGolem(GuiGraphics g, String snbt, int cx, int feetY, float scale, int clipX, int clipY, int clipW, int clipH) {
        GolemEntity golem = golems.computeIfAbsent(snbt, s -> {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return null;
            }
            GolemEntity e = ModEntities.GOLEM.get().create(level);
            if (e == null) {
                return null;
            }
            try {
                CompoundTag tag = TagParser.parseTag(s);
                e.importData(tag);
            } catch (Exception ex) {
                GolemGarage.LOGGER.warn("Bad golem preview in the manual: {}", s, ex);
            }
            return e;
        });
        if (golem == null) {
            return;
        }
        float angle = (net.minecraft.Util.getMillis() % 12000L) / 12000f * 360f;
        golem.yBodyRot = 180f + angle;
        golem.yBodyRotO = golem.yBodyRot;
        golem.setYRot(golem.yBodyRot);
        golem.yRotO = golem.yBodyRot;
        golem.yHeadRot = golem.yBodyRot;
        golem.yHeadRotO = golem.yBodyRot;
        golem.setXRot(0);
        golem.xRotO = 0;
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf tilt = new Quaternionf().rotateX(-12f * Mth.DEG_TO_RAD);
        pose.mul(tilt);
        g.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);
        InventoryScreen.renderEntityInInventory(g, cx, feetY, scale, new Vector3f(), pose, tilt, golem);
        g.disableScissor();
    }
}
