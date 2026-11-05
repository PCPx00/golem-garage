package com.xtrip.golemgarage.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Adds the in-game settings screen (Mods list, Golem Garage, Config). */
public final class ConfigScreenHook {
    private ConfigScreenHook() {
    }

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
