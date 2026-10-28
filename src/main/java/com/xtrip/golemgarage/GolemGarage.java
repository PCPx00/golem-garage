package com.xtrip.golemgarage;

import com.mojang.logging.LogUtils;
import com.xtrip.golemgarage.block.ModBlocks;
import com.xtrip.golemgarage.block.entity.ModBlockEntities;
import com.xtrip.golemgarage.entity.ModEntities;
import com.xtrip.golemgarage.item.ModCreativeTabs;
import com.xtrip.golemgarage.item.ModDataComponents;
import com.xtrip.golemgarage.item.ModItems;
import com.xtrip.golemgarage.particle.ModParticles;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(GolemGarage.MOD_ID)
public class GolemGarage {
    public static final String MOD_ID = "golemgarage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GolemGarage(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        com.xtrip.golemgarage.menu.ModMenus.MENUS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        com.xtrip.golemgarage.advancement.ModTriggers.TRIGGERS.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributes);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, Config.SERVER_SPEC);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        LOGGER.info("Golem Garage loaded");
    }
}
