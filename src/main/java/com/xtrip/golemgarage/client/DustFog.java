package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.xtrip.golemgarage.GolemGarage;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** A dust storm closes the fog in to a tan haze, fading in and out over a few seconds. */
@EventBusSubscriber(modid = GolemGarage.MOD_ID, value = Dist.CLIENT)
public final class DustFog {
    private static boolean on;
    private static float amount, amountO;

    private DustFog() {
    }

    public static void set(boolean storm) {
        on = storm;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        amountO = amount;
        amount = Mth.clamp(amount + (on ? 0.02f : -0.02f), 0f, 1f);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        float a = Mth.lerp((float) event.getPartialTick(), amountO, amount);
        if (a <= 0) {
            return;
        }
        float far = Mth.lerp(a, event.getFarPlaneDistance(), 28f);
        event.setNearPlaneDistance(Mth.lerp(a, event.getNearPlaneDistance(), 2f));
        event.setFarPlaneDistance(far);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onColor(ViewportEvent.ComputeFogColor event) {
        float a = Mth.lerp((float) event.getPartialTick(), amountO, amount);
        if (a <= 0) {
            return;
        }
        event.setRed(Mth.lerp(a, event.getRed(), 0.78f));
        event.setGreen(Mth.lerp(a, event.getGreen(), 0.66f));
        event.setBlue(Mth.lerp(a, event.getBlue(), 0.46f));
    }
}
