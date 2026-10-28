package com.xtrip.golemgarage.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

/** Draws a golem or cub inside a screen without its floating name tag or status icon. */
public final class GuiPreview {
    static boolean active;

    private GuiPreview() {
    }

    public static void render(GuiGraphics g, int x1, int y1, int x2, int y2, int scale, int mouseX, int mouseY,
                              LivingEntity entity) {
        active = true;
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, scale, 0.0625f, mouseX, mouseY, entity);
        } finally {
            active = false;
        }
    }
}
