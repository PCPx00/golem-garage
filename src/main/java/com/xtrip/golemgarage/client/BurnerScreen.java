package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.block.GeneratorInfo;
import com.xtrip.golemgarage.power.BurnerGeneratorBlockEntity;
import com.xtrip.golemgarage.power.BurnerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.List;

/** The Scrap Burner: lava tank on the left, fuel in the middle, a readout, and the energy gauge on the right. */
public class BurnerScreen extends AbstractContainerScreen<BurnerMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath("golemgarage",
            "textures/gui/burner_generator.png");
    private static final int TITLE = 0xFFFFC14A, TEXT = 0xFFE6ECF4, DIM = 0xFF8A96A8, GOOD = 0xFF6CFF8A, WARN = 0xFFFF9A5A;
    private static final int TANK_X = 10, TANK_Y = 19, TANK_W = 16, TANK_H = 58;
    private static final int BAR_X = 152, BAR_Y = 19, BAR_W = 14, BAR_H = 58;
    private static final int TEXT_X = 68, TEXT_W = 76;

    public BurnerScreen(BurnerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 174;
        inventoryLabelY = 80;
        titleLabelY = 5;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (in(x, y, TANK_X, TANK_Y, TANK_W, TANK_H)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("block.minecraft.lava"),
                    Component.translatable("golemgarage.power.mb", String.format("%,d", menu.lava()),
                            String.format("%,d", BurnerGeneratorBlockEntity.TANK)).withStyle(s -> s.withColor(DIM))), mouseX, mouseY);
        } else if (in(x, y, BAR_X, BAR_Y, BAR_W, BAR_H)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("golemgarage.power.energy"),
                    Component.translatable("golemgarage.power.fe_of", String.format("%,d", menu.energy()), "100,000")
                            .withStyle(s -> s.withColor(DIM))), mouseX, mouseY);
        } else if (in(x, y, 46, 19, 14, 14) && menu.burn() > 0) {
            g.renderTooltip(font, Component.translatable("golemgarage.power.burn_left", (menu.burn() + 19) / 20), mouseX, mouseY);
        }
    }

    private static boolean in(int x, int y, int x0, int y0, int w, int h) {
        return x >= x0 && x < x0 + w && y >= y0 && y < y0 + h;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x0 = leftPos, y0 = topPos;
        g.blit(BG, x0, y0, 0, 0, imageWidth, imageHeight, 256, 256);
        // Lava, drawn with the real animated lava texture.
        int lava = menu.lava();
        if (lava > 0) {
            int h = Math.max(1, Math.round(TANK_H * lava / (float) BurnerGeneratorBlockEntity.TANK));
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ResourceLocation.withDefaultNamespace("block/lava_still"));
            int top = y0 + TANK_Y + TANK_H - h;
            g.enableScissor(x0 + TANK_X, top, x0 + TANK_X + TANK_W, y0 + TANK_Y + TANK_H);
            for (int yy = y0 + TANK_Y + TANK_H - 16; yy > top - 16; yy -= 16) {
                g.blit(x0 + TANK_X, yy, 0, 16, 16, sprite);
            }
            g.disableScissor();
        }
        // Energy: a bar that warms from red to green as it fills.
        float e = menu.energy() / 100_000f;
        int eh = Math.round(BAR_H * Math.min(1, e));
        if (eh > 0) {
            int col = e > 0.5f ? 0xFF59E07A : e > 0.2f ? 0xFFF0C23A : 0xFFE0563A;
            int top = y0 + BAR_Y + BAR_H - eh;
            g.fill(x0 + BAR_X, top, x0 + BAR_X + BAR_W, y0 + BAR_Y + BAR_H, col);
            g.fill(x0 + BAR_X, top, x0 + BAR_X + 3, y0 + BAR_Y + BAR_H, 0x40FFFFFF);
            g.fill(x0 + BAR_X, top, x0 + BAR_X + BAR_W, top + 1, 0x80FFFFFF);
        }
        // The flame burns down like a furnace's.
        if (menu.burn() > 0 && menu.burnMax() > 0) {
            int fh = Math.max(1, Math.round(14 * menu.burn() / (float) menu.burnMax()));
            g.blit(BG, x0 + 46, y0 + 19 + 14 - fh, 176, 14 - fh, 14, fh, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, titleLabelY, TITLE, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, DIM, false);
        int rate = menu.rate();
        boolean full = menu.energy() >= 100_000;
        Component status;
        int col;
        if (menu.burn() > 0 && full) {
            status = Component.translatable("golemgarage.power.full");
            col = WARN;
        } else if (menu.burn() > 0) {
            status = Component.translatable(rate >= BurnerGeneratorBlockEntity.LAVA_RATE
                    ? "golemgarage.power.burning_lava" : "golemgarage.power.burning");
            col = GOOD;
        } else {
            status = Component.translatable("golemgarage.power.no_fuel");
            col = DIM;
        }
        int y = 22;
        line(g, status.getString(), y, col);
        y += 12;
        line(g, "+" + rate + " FE/t", y, rate > 0 ? GOOD : DIM);
        y += 16;
        line(g, Component.translatable("golemgarage.power.stored").getString(), y, DIM);
        y += 10;
        line(g, GeneratorInfo.fe(menu.energy()) + " FE", y, TEXT);
    }

    private void line(GuiGraphics g, String s, int y, int col) {
        g.drawString(font, font.plainSubstrByWidth(s, TEXT_W), TEXT_X, y, col, false);
    }
}
