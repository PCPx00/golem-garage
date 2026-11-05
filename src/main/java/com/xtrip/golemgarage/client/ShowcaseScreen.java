package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.menu.ShowcaseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** The Garage showcase: six pedestal slots (the ones this Garage's size allows are open) above your inventory. */
public class ShowcaseScreen extends AbstractContainerScreen<ShowcaseMenu> {
    private static final int BG = 0xFF2C2F36, EDGE_HI = 0xFF6A6E78, EDGE_LO = 0xFF15161A, TITLE = 0xFFFFC14A,
            TEXT = 0xFFE6ECF4, DIM = 0xFF9AA4B4;

    public ShowcaseScreen(ShowcaseMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 174;
        inventoryLabelY = 80;
        titleLabelY = 6;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        // Locked slots explain themselves.
        for (int i = 0; i < ShowcaseMenu.SLOTS; i++) {
            Slot s = menu.getSlot(i);
            if (i >= menu.unlocked() && isHovering(s.x, s.y, 16, 16, mouseX, mouseY)) {
                g.renderComponentTooltip(font, List.of(Component.translatable("golemgarage.showcase.locked"),
                        Component.translatable("golemgarage.showcase.locked_tip").withStyle(st -> st.withColor(DIM))),
                        mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x0 = leftPos, y0 = topPos;
        g.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, BG);
        g.fill(x0, y0, x0 + imageWidth, y0 + 1, EDGE_HI);
        g.fill(x0, y0, x0 + 1, y0 + imageHeight, EDGE_HI);
        g.fill(x0, y0 + imageHeight - 1, x0 + imageWidth, y0 + imageHeight, EDGE_LO);
        g.fill(x0 + imageWidth - 1, y0, x0 + imageWidth, y0 + imageHeight, EDGE_LO);
        for (int x = x0 + 4; x < x0 + imageWidth - 4; x++) {
            g.fill(x, y0 + 16, x + 1, y0 + 18, ((x - x0) / 3) % 2 == 0 ? 0xFFDEAC2E : 0xFF1E1C1E);
        }
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.getSlot(i);
            boolean locked = i < ShowcaseMenu.SLOTS && i >= menu.unlocked();
            int sx = x0 + s.x - 1, sy = y0 + s.y - 1;
            if (i < ShowcaseMenu.SLOTS) {
                // A little plinth under each showcase slot.
                g.fill(sx - 2, sy + 18, sx + 20, sy + 21, locked ? 0xFF22242A : 0xFF8A6A3A);
            }
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
            g.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, locked ? 0xFF4A4C52 : 0xFF8B8B8B);
            if (locked) {
                // Padlock.
                g.fill(sx + 5, sy + 8, sx + 13, sy + 14, 0xFF2A2B30);
                g.fill(sx + 6, sy + 4, sx + 7, sy + 8, 0xFF2A2B30);
                g.fill(sx + 11, sy + 4, sx + 12, sy + 8, 0xFF2A2B30);
                g.fill(sx + 6, sy + 4, sx + 12, sy + 5, 0xFF2A2B30);
                g.fill(sx + 8, sy + 10, sx + 10, sy + 12, 0xFF6A6E78);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, titleLabelY, TITLE, false);
        Component open = Component.translatable("golemgarage.showcase.unlocked", menu.unlocked(), ShowcaseMenu.SLOTS);
        g.drawString(font, open, (imageWidth - font.width(open)) / 2, 56, TEXT, false);
        Component hint = Component.translatable("golemgarage.showcase.hint");
        g.drawString(font, font.plainSubstrByWidth(hint.getString(), imageWidth - 12),
                Math.max(6, (imageWidth - font.width(hint)) / 2), 66, DIM, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, DIM, false);
    }
}
