package com.xtrip.golemgarage.power;

import com.xtrip.golemgarage.block.BurnerGeneratorBlock;
import com.xtrip.golemgarage.block.ModBlocks;
import com.xtrip.golemgarage.block.SolarPanelBlock;
import com.xtrip.golemgarage.block.WindTurbineBlock;
import com.xtrip.golemgarage.block.entity.ModBlockEntities;
import com.xtrip.golemgarage.item.ModItems;
import com.xtrip.golemgarage.menu.ModMenus;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** The power blocks: Solar Panel, Wind Turbine and Scrap Burner, and their block entities, items and menu. */
public final class PowerBlocks {
    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL = ModBlocks.BLOCKS.register("solar_panel",
            () -> new SolarPanelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE)
                    .strength(2f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));
    public static final DeferredBlock<WindTurbineBlock> WIND_TURBINE = ModBlocks.BLOCKS.register("wind_turbine",
            () -> new WindTurbineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(3f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));
    public static final DeferredBlock<BurnerGeneratorBlock> BURNER = ModBlocks.BLOCKS.register("burner_generator",
            () -> new BurnerGeneratorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(BurnerGeneratorBlock.LIT) ? 13 : 0)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("solar_panel",
                    () -> BlockEntityType.Builder.of(SolarPanelBlockEntity::new, SOLAR_PANEL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindTurbineBlockEntity>> WIND_TURBINE_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("wind_turbine",
                    () -> BlockEntityType.Builder.of(WindTurbineBlockEntity::new, WIND_TURBINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BurnerGeneratorBlockEntity>> BURNER_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("burner_generator",
                    () -> BlockEntityType.Builder.of(BurnerGeneratorBlockEntity::new, BURNER.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<BurnerMenu>> BURNER_MENU =
            ModMenus.MENUS.register("burner_generator", () -> IMenuTypeExtension.create(BurnerMenu::fromNetwork));

    public static final DeferredItem<BlockItem> SOLAR_PANEL_ITEM = ModItems.extraBlock("solar_panel", SOLAR_PANEL);
    public static final DeferredItem<BlockItem> WIND_TURBINE_ITEM = ModItems.extraBlock("wind_turbine", WIND_TURBINE);
    public static final DeferredItem<BlockItem> BURNER_ITEM = ModItems.extraBlock("burner_generator", BURNER);

    private PowerBlocks() {
    }

    /** Loads the class so everything above is registered. */
    public static void init() {
    }

    /** Every side takes pipes: energy out; the burner also takes fuel, lava, and gives back empty buckets. */
    public static void capabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SOLAR_PANEL_BE.get(), (be, side) -> be.energy());
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, WIND_TURBINE_BE.get(), (be, side) -> be.energy());
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BURNER_BE.get(), (be, side) -> be.energy());
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BURNER_BE.get(), (be, side) -> be.pipeItems());
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BURNER_BE.get(), (be, side) -> be.pipeFluid());
    }
}
