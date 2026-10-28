package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.entity.CubEntity;
import com.xtrip.golemgarage.item.OutfitItem;
import com.xtrip.golemgarage.menu.CubMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The Cub's wardrobe and pockets, with a live preview of the Cub in its outfit. */
public class CubScreen extends AbstractContainerScreen<CubMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath("golemgarage",
            "textures/gui/cub_wardrobe.png");
    private static final int TEXT = 0xFF5A4050;

    public CubScreen(CubMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 196;
        inventoryLabelY = 103;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        CubEntity cub = menu.cub();
        if (cub != null) {
            GuiPreview.render(g, leftPos + 9, topPos + 17, leftPos + 57, topPos + 79, 26, mouseX, mouseY, cub);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        g.drawString(font, Component.translatable("screen.golemgarage.cub.pockets"), 8, 80, TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
        // Info card: what it's wearing and any set bonus.
        int x = 88;
        int y = 20;
        g.drawString(font, Component.translatable("screen.golemgarage.cub.wardrobe"), x, y, 0xFFB04880, false);
        y += 12;
        String theme = null;
        boolean full = true;
        for (int i = 0; i < CubMenu.WARDROBE; i++) {
            ItemStack s = menu.getSlot(i).getItem();
            String name = s.isEmpty() ? "-" : s.getHoverName().getString();
            g.drawString(font, font.plainSubstrByWidth(name, 78), x, y, s.isEmpty() ? 0xFFB0A0A8 : TEXT, false);
            y += 10;
            if (s.getItem() instanceof OutfitItem o) {
                if (theme == null) {
                    theme = o.theme();
                } else if (!theme.equals(o.theme())) {
                    full = false;
                }
            } else {
                full = false;
            }
        }
        if (full && theme != null) {
            Component set = Component.translatable("screen.golemgarage.cub.full_set",
                    Component.translatable("outfit.golemgarage.theme." + theme));
            g.drawString(font, set, x, 70, 0xFF2E9A5A, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }
}
