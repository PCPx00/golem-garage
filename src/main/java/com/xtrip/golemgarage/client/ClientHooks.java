package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.entity.GolemEntity;
import net.minecraft.client.Minecraft;

/** Client-only entry points, kept out of common classes so servers never load Screen code. */
public final class ClientHooks {
    public static void openGolemScreen(GolemEntity golem) {
        Minecraft.getInstance().setScreen(new GolemScreen(golem));
    }

    public static void openTablet() {
        Minecraft.getInstance().setScreen(new TabletScreen());
    }

    public static void tabletData(com.xtrip.golemgarage.tablet.TabletPayloads.Data data) {
        TabletScreen.receive(data);
    }

    public static void openManual() {
        Minecraft.getInstance().setScreen(new com.xtrip.golemgarage.client.manual.ManualScreen());
    }

    private ClientHooks() {
    }

    public static void openGaragePlanner(net.minecraft.core.BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new GaragePlannerScreen(pos));
    }

    public static void storm(boolean on) {
        DustFog.set(on);
    }
}
