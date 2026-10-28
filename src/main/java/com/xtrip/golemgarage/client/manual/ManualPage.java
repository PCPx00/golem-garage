package com.xtrip.golemgarage.client.manual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.core.NonNullList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** One page of the manual. Pages are laid out once per entry; text that doesn't fit spills onto extra pages. */
public abstract class ManualPage {
    public static final int TEXT = 0xFF3A3226;
    public static final int TITLE = 0xFF1F3B63;
    public static final int MUTED = 0xFF7D7262;
    public static final int LINE_H = 9;
    static final int TITLE_H = 16;

    /** Optional heading drawn at the top of the page. */
    protected String title;
    /** Wrapped body text drawn below the page's own content. */
    protected List<FormattedCharSequence> lines = new ArrayList<>();

    /** Height of the page's own content (picture, grid...) between the title and the text. */
    protected int contentHeight(int w) {
        return 0;
    }

    /** Draws the page's own content at (x, y). */
    protected void renderContent(ManualScreen.Ctx ctx, int x, int y, int w) {
    }

    public void render(ManualScreen.Ctx ctx, int x, int y, int w, int h) {
        int cy = y;
        if (title != null && !title.isEmpty()) {
            drawTitle(ctx, title, x, cy, w);
            cy += TITLE_H;
        }
        int ch = contentHeight(w);
        if (ch > 0) {
            renderContent(ctx, x, cy, w);
            cy += ch + 4;
        }
        for (FormattedCharSequence line : lines) {
            ctx.g.drawString(ctx.font, line, x, cy, TEXT, false);
            cy += LINE_H;
        }
    }

    /** Turns text with section-sign codes into real styled pieces, so styles survive line wrapping. */
    public static Component styled(String text) {
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                if (run.length() > 0) {
                    out.append(Component.literal(run.toString()).setStyle(style));
                    run.setLength(0);
                }
                net.minecraft.ChatFormatting f = net.minecraft.ChatFormatting.getByCode(text.charAt(++i));
                if (f == null || f == net.minecraft.ChatFormatting.RESET) {
                    style = net.minecraft.network.chat.Style.EMPTY;
                } else {
                    style = style.applyLegacyFormat(f);
                }
                continue;
            }
            run.append(c);
        }
        if (run.length() > 0) {
            out.append(Component.literal(run.toString()).setStyle(style));
        }
        return out;
    }

    static void drawTitle(ManualScreen.Ctx ctx, String text, int x, int y, int w) {
        Component c = Component.literal(text).withStyle(s -> s.withBold(true));
        int tw = ctx.font.width(c);
        if (tw <= w) {
            ctx.g.drawString(ctx.font, c, x + (w - tw) / 2, y, TITLE, false);
        } else {
            float scale = (float) w / tw;
            var pose = ctx.g.pose();
            pose.pushPose();
            pose.translate(x, y + (8 - 8 * scale) / 2f, 0);
            pose.scale(scale, scale, 1f);
            ctx.g.drawString(ctx.font, c, 0, 0, TITLE, false);
            pose.popPose();
        }
        ctx.screen.drawDivider(ctx.g, x + w / 2, y + 11);
    }

    // =================================================================== building
    /** Builds one JSON page into one or more laid-out pages. */
    public static List<ManualPage> build(JsonObject o, Font font, int w, int h) {
        String type = ManualBook.str(o, "type", "text");
        ManualPage page = switch (type) {
            case "spotlight" -> new Spotlight(ManualBook.stack(ManualBook.str(o, "item", "minecraft:book")));
            case "crafting" -> new Crafting(ManualBook.str(o, "recipe", ""));
            case "golem" -> new Golem(ManualBook.str(o, "golem", "{}"), o.has("height") ? o.get("height").getAsInt() : 92);
            case "image" -> new Image(ResourceLocation.parse(ManualBook.str(o, "texture", "")),
                    o.get("width").getAsInt(), o.get("height").getAsInt());
            case "items" -> new Items(o.getAsJsonArray("items"), o.has("links") ? o.getAsJsonArray("links") : null);
            case "table" -> new Table(o.getAsJsonArray("rows"));
            default -> new ManualPage() {
            };
        };
        page.title = ManualBook.str(o, "title", null);
        if (page instanceof Table table) {
            return table.paginate(font, w, h);
        }
        List<FormattedCharSequence> all = font.split(styled(ManualBook.str(o, "text", "")), w);
        if (all.size() == 1 && font.width(all.get(0)) == 0) {
            all = List.of();
        }
        int top = (page.title != null && !page.title.isEmpty() ? TITLE_H : 0);
        int ch = page.contentHeight(w);
        if (ch > 0) {
            top += ch + 4;
        }
        int fit = Math.max(0, (h - top) / LINE_H);
        List<ManualPage> out = new ArrayList<>();
        page.lines = new ArrayList<>(all.subList(0, Math.min(fit, all.size())));
        out.add(page);
        int i = page.lines.size();
        int perPage = h / LINE_H;
        while (i < all.size()) {
            ManualPage more = new ManualPage() {
            };
            // Don't start a continuation page with a blank line.
            while (i < all.size() && ctxWidth(font, all.get(i)) == 0) {
                i++;
            }
            int end = Math.min(all.size(), i + perPage);
            more.lines = new ArrayList<>(all.subList(i, end));
            if (!more.lines.isEmpty()) {
                out.add(more);
            }
            i = end;
        }
        return out;
    }

    private static int ctxWidth(Font font, FormattedCharSequence s) {
        return font.width(s);
    }

    // =================================================================== spotlight
    static final class Spotlight extends ManualPage {
        final ItemStack stack;

        Spotlight(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        protected int contentHeight(int w) {
            return 40;
        }

        @Override
        protected void renderContent(ManualScreen.Ctx ctx, int x, int y, int w) {
            int cx = x + w / 2;
            ctx.screen.drawFrame(ctx.g, cx - 19, y + 1, 38, 38);
            var pose = ctx.g.pose();
            pose.pushPose();
            pose.translate(cx - 16, y + 4, 0);
            pose.scale(2f, 2f, 1f);
            ctx.g.renderItem(stack, 0, 0);
            pose.popPose();
            ctx.hot(cx - 19, y + 1, 38, 38, stack, null);
        }
    }

    // =================================================================== crafting
    static final class Crafting extends ManualPage {
        final String recipeId;

        Crafting(String recipeId) {
            this.recipeId = recipeId;
        }

        @Override
        protected int contentHeight(int w) {
            return 62;
        }

        private Optional<RecipeHolder<?>> recipe() {
            var level = Minecraft.getInstance().level;
            if (level == null || recipeId.isEmpty()) {
                return Optional.empty();
            }
            return level.getRecipeManager().byKey(ResourceLocation.parse(recipeId));
        }

        @Override
        public void render(ManualScreen.Ctx ctx, int x, int y, int w, int h) {
            if (title == null) {
                recipe().ifPresent(r -> title = r.value().getResultItem(ctx.registries()).getHoverName().getString());
            }
            super.render(ctx, x, y, w, h);
        }

        @Override
        protected void renderContent(ManualScreen.Ctx ctx, int x, int y, int w) {
            Optional<RecipeHolder<?>> holder = recipe();
            int gx = x + (w - 112) / 2;
            int gy = y + 3;
            if (holder.isEmpty()) {
                List<FormattedCharSequence> msg = ctx.font.split(Component.literal(
                        net.minecraft.client.resources.language.I18n.get("screen.golemgarage.manual.no_recipe")), w - 8);
                int my = y + 14;
                for (FormattedCharSequence l : msg) {
                    ctx.g.drawString(ctx.font, l, x + 4, my, 0xFF8A2E2E, false);
                    my += LINE_H;
                }
                return;
            }
            Recipe<?> recipe = holder.get().value();
            NonNullList<Ingredient> ings = recipe.getIngredients();
            int cols = 3;
            int rows = 3;
            boolean shaped = recipe instanceof ShapedRecipe;
            if (shaped) {
                cols = ((ShapedRecipe) recipe).getWidth();
                rows = ((ShapedRecipe) recipe).getHeight();
            }
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int sx = gx + c * 18;
                    int sy = gy + r * 18;
                    ctx.screen.drawSlot(ctx.g, sx, sy);
                    int idx;
                    if (shaped) {
                        idx = (r < rows && c < cols) ? r * cols + c : -1;
                    } else {
                        idx = r * 3 + c;
                    }
                    if (idx >= 0 && idx < ings.size()) {
                        ItemStack[] options = ings.get(idx).getItems();
                        if (options.length > 0) {
                            ItemStack s = options[(int) ((ctx.time / 1000) % options.length)];
                            ctx.g.renderItem(s, sx + 1, sy + 1);
                            ctx.hot(sx, sy, 18, 18, s, null);
                        }
                    }
                }
            }
            ctx.screen.drawArrow(ctx.g, gx + 58, gy + 20);
            ItemStack result = recipe.getResultItem(ctx.registries());
            int rx = gx + 82;
            int ry = gy + 14;
            ctx.screen.drawFrame(ctx.g, rx, ry, 26, 26);
            ctx.g.renderItem(result, rx + 5, ry + 5);
            ctx.g.renderItemDecorations(ctx.font, result, rx + 5, ry + 5);
            ctx.hot(rx, ry, 26, 26, result, null);
            String kind = net.minecraft.client.resources.language.I18n.get(shaped ? "screen.golemgarage.manual.shaped" : "screen.golemgarage.manual.shapeless");
            ctx.g.drawString(ctx.font, kind, gx + 82 + 13 - ctx.font.width(kind) / 2, gy + 44, MUTED, false);
        }
    }

    // =================================================================== golem preview
    static final class Golem extends ManualPage {
        final String snbt;
        final int height;

        Golem(String snbt, int height) {
            this.snbt = snbt;
            this.height = height;
        }

        @Override
        protected int contentHeight(int w) {
            return height;
        }

        @Override
        protected void renderContent(ManualScreen.Ctx ctx, int x, int y, int w) {
            ctx.screen.drawPedestal(ctx.g, x + w / 2, y + height - 6);
            ctx.screen.renderGolem(ctx.g, snbt, x + w / 2, y + height - 7, height * 0.33f, x, y, w, height);
        }
    }

    // =================================================================== image
    static final class Image extends ManualPage {
        final ResourceLocation texture;
        final int tw;
        final int th;

        Image(ResourceLocation texture, int tw, int th) {
            this.texture = texture;
            this.tw = tw;
            this.th = th;
        }

        @Override
        protected int contentHeight(int w) {
            int dw = Math.min(w, tw);
            return th * dw / tw;
        }

        @Override
        protected void renderContent(ManualScreen.Ctx ctx, int x, int y, int w) {
            int dw = Math.min(w, tw);
            int dh = th * dw / tw;
            ctx.g.blit(texture, x + (w - dw) / 2, y, dw, dh, 0, 0, tw, th, tw, th);
        }
    }

    // =================================================================== item grid
    static final class Items extends ManualPage {
        final List<ItemStack> stacks = new ArrayList<>();
        final List<String> links = new ArrayList<>();

        Items(JsonArray items, JsonArray linkArray) {
            for (JsonElement e : items) {
                stacks.add(ManualBook.stack(e.getAsString()));
            }
            if (linkArray != null) {
                for (JsonElement e : linkArray) {
                    links.add(e.isJsonNull() ? null : e.getAsString());
                }
            }
        }

        private int cols(int w) {
            return Math.max(1, w / 20);
        }

        @Override
        protected int contentHeight(int w) {
            int rows = (stacks.size() + cols(w) - 1) / cols(w);
            return rows * 20;
        }

        @Override
        protected void renderContent(ManualScreen.Ctx ctx, int x, int y, int w) {
            int cols = cols(w);
            int used = Math.min(cols, stacks.size());
            int ox = x + (w - used * 20) / 2;
            for (int i = 0; i < stacks.size(); i++) {
                int sx = ox + (i % cols) * 20;
                int sy = y + (i / cols) * 20;
                ctx.screen.drawSlot(ctx.g, sx, sy);
                ctx.g.renderItem(stacks.get(i), sx + 1, sy + 1);
                ctx.hot(sx, sy, 18, 18, stacks.get(i), i < links.size() ? links.get(i) : null);
            }
        }
    }

    // =================================================================== table
    static final class Table extends ManualPage {
        final List<String[]> rows = new ArrayList<>();
        /** Laid-out rows: label lines and value lines. */
        final List<List<FormattedCharSequence>> labels = new ArrayList<>();
        final List<List<FormattedCharSequence>> values = new ArrayList<>();

        Table(JsonArray array) {
            for (JsonElement e : array) {
                JsonArray r = e.getAsJsonArray();
                rows.add(new String[]{r.get(0).getAsString(), r.size() > 1 ? r.get(1).getAsString() : ""});
            }
        }

        private Table(String title) {
            this.title = title;
        }

        List<ManualPage> paginate(Font font, int w, int h) {
            List<ManualPage> out = new ArrayList<>();
            Table current = new Table(title);
            int used = title != null ? TITLE_H : 0;
            for (String[] r : rows) {
                Component label = Component.literal(r[0]).withStyle(s -> s.withBold(true).withColor(TITLE & 0xFFFFFF));
                Component inline = Component.empty().append(label).append(Component.literal(": " + r[1])
                        .withStyle(s -> s.withColor(TEXT & 0xFFFFFF)));
                List<FormattedCharSequence> l;
                List<FormattedCharSequence> v;
                if (!r[1].isEmpty() && font.width(inline) <= w) {
                    l = List.of(inline.getVisualOrderText());
                    v = List.of();
                } else {
                    l = font.split(label, w);
                    v = r[1].isEmpty() ? List.of() : font.split(styled(r[1]), w - 6);
                }
                int rh = (l.size() + v.size()) * LINE_H + 3;
                if (used + rh > h && !current.labels.isEmpty()) {
                    out.add(current);
                    current = new Table(title == null ? null : title + " (cont.)");
                    used = title != null ? TITLE_H : 0;
                }
                current.labels.add(l);
                current.values.add(v);
                used += rh;
            }
            out.add(current);
            return out;
        }

        @Override
        public void render(ManualScreen.Ctx ctx, int x, int y, int w, int h) {
            int cy = y;
            if (title != null && !title.isEmpty()) {
                drawTitle(ctx, title, x, cy, w);
                cy += TITLE_H;
            }
            for (int i = 0; i < labels.size(); i++) {
                for (FormattedCharSequence l : labels.get(i)) {
                    ctx.g.drawString(ctx.font, l, x, cy, TITLE, false);
                    cy += LINE_H;
                }
                for (FormattedCharSequence v : values.get(i)) {
                    ctx.g.drawString(ctx.font, v, x + 6, cy, TEXT, false);
                    cy += LINE_H;
                }
                cy += 3;
            }
        }
    }
}
