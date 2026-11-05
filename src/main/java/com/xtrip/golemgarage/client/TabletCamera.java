package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.GolemGarage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Watching through a golem's eyes from the tablet. Sneak (or the golem leaving range) returns you. */
@EventBusSubscriber(modid = GolemGarage.MOD_ID, value = Dist.CLIENT)
public final class TabletCamera {
    private static Entity watching;

    private TabletCamera() {
    }

    static void watch(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        watching = e;
        mc.setCameraEntity(e);
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("golemgarage.tablet.watching", e.getName()), true);
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (watching == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !watching.isAlive() || watching.isRemoved() || mc.options.keyShift.isDown()
                || mc.getCameraEntity() != watching) {
            if (mc.player != null && mc.getCameraEntity() == watching) {
                mc.setCameraEntity(mc.player);
            }
            watching = null;
        }
    }
}
