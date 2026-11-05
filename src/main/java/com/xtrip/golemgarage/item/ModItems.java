package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.block.ModBlocks;
import com.xtrip.golemgarage.golem.PartShape;
import com.xtrip.golemgarage.golem.PartShapes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GolemGarage.MOD_ID);
    /** Newer items, shown at the end of the main creative tab in this order. */
    public static final java.util.List<DeferredItem<? extends Item>> EXTRAS = new java.util.ArrayList<>();

    private static <T extends Item> DeferredItem<T> extra(DeferredItem<T> item) {
        EXTRAS.add(item);
        return item;
    }

    public static final DeferredItem<BlockItem> GARAGE_CONTROLLER =
            ITEMS.registerSimpleBlockItem("garage_controller", ModBlocks.GARAGE_CONTROLLER);
    public static final DeferredItem<BlockItem> LASER_EMITTER =
            ITEMS.registerSimpleBlockItem("laser_emitter", ModBlocks.LASER_EMITTER);
    public static final DeferredItem<BlockItem> STEEL_EMITTER =
            ITEMS.registerSimpleBlockItem("steel_emitter", ModBlocks.STEEL_EMITTER);
    public static final DeferredItem<BlockItem> SIGNAL_PLATE =
            ITEMS.registerSimpleBlockItem("signal_plate", ModBlocks.SIGNAL_PLATE);
    public static final DeferredItem<BlockItem> GARAGE_DOOR =
            ITEMS.registerSimpleBlockItem("garage_door", ModBlocks.GARAGE_DOOR);
    public static final DeferredItem<BlockItem> GARAGE_WINDOW =
            ITEMS.registerSimpleBlockItem("garage_window", ModBlocks.GARAGE_WINDOW);
    public static final DeferredItem<BlockItem> GOLEM_DOCK =
            ITEMS.registerSimpleBlockItem("golem_dock", ModBlocks.GOLEM_DOCK);

    public static final DeferredItem<GolemItem> GOLEM = ITEMS.registerItem("golem", GolemItem::new);
    public static final DeferredItem<CubItem> CUB = ITEMS.registerItem("cub", CubItem::new);
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    /** Restores a quarter of every part's durability and health. */
    public static final DeferredItem<Item> REPAIR_KIT = ITEMS.registerSimpleItem("repair_kit");
    public static final DeferredItem<BlueprintItem> BLUEPRINT = ITEMS.registerItem("blueprint", BlueprintItem::new);
    public static final DeferredItem<GolemLinkerItem> GOLEM_LINKER = ITEMS.registerItem("golem_linker", GolemLinkerItem::new);
    /** Salvage from wrecks, raiders and scrapyards; builds and repairs rigs. */
    public static final DeferredItem<Item> SCRAP = extra(ITEMS.registerSimpleItem("scrap"));
    public static final DeferredItem<Item> TYRE = extra(ITEMS.registerSimpleItem("tyre"));
    /** The Wasteland Rig, packed up. Use it on the ground to drive. */
    public static final DeferredItem<RigItem> RIG = extra(ITEMS.registerItem("rig", RigItem::new, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> RIG_RAM = extra(ITEMS.registerSimpleItem("rig_ram"));
    public static final DeferredItem<Item> RIG_TURRET = extra(ITEMS.registerSimpleItem("rig_turret"));
    public static final DeferredItem<Item> RIG_PLATING = extra(ITEMS.registerSimpleItem("rig_plating"));
    /** A punch card holding a golem's rules. */
    public static final DeferredItem<RuleCardItem> RULE_CARD = extra(ITEMS.registerItem("rule_card", RuleCardItem::new));
    /** Commands every golem nearby: rally, hold or charge. */
    public static final DeferredItem<WarHornItem> WAR_HORN = extra(ITEMS.registerItem("war_horn", WarHornItem::new,
            new Item.Properties().stacksTo(1)));
    /** Spray cans, one per finish. */
    public static final Map<String, DeferredItem<SprayCanItem>> SPRAY_CANS = new LinkedHashMap<>();

    static {
        for (String f : SprayCanItem.FINISHES) {
            SPRAY_CANS.put(f, extra(ITEMS.registerItem("spray_can_" + f, p -> new SprayCanItem(f, p))));
        }
    }

    public static final DeferredItem<BlockItem> FABRICATOR = extra(ITEMS.registerSimpleBlockItem("fabricator", ModBlocks.FABRICATOR));
    public static final DeferredItem<GolemTabletItem> GOLEM_TABLET = extra(ITEMS.registerItem("golem_tablet", GolemTabletItem::new,
            new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> CUB_BED = extra(ITEMS.registerSimpleBlockItem("cub_bed", ModBlocks.CUB_BED));
    public static final DeferredItem<BlockItem> GARAGE_RADIO = extra(ITEMS.registerSimpleBlockItem("garage_radio", ModBlocks.GARAGE_RADIO));
    /** The in-game guide book. */
    public static final DeferredItem<ManualItem> MANUAL = ITEMS.registerItem("manual", ManualItem::new);

    /** Upgrade modules and the casing they're built in. */
    public static final DeferredItem<Item> MODULE_CASING = ITEMS.registerSimpleItem("module_casing");
    public static final Map<com.xtrip.golemgarage.golem.GolemModule, DeferredItem<ModuleItem>> MODULES =
            new java.util.EnumMap<>(com.xtrip.golemgarage.golem.GolemModule.class);

    static {
        for (com.xtrip.golemgarage.golem.GolemModule m : com.xtrip.golemgarage.golem.GolemModule.values()) {
            MODULES.put(m, ITEMS.registerItem(m.itemId(), p -> new ModuleItem(m, p)));
        }
    }

    /** Cub outfit pieces, by outfit id. */
    public static final Map<String, DeferredItem<OutfitItem>> OUTFITS = new LinkedHashMap<>();

    static {
        for (String[] o : OutfitPieces.ALL) {
            OUTFITS.put(o[0], ITEMS.registerItem("outfit_" + o[0],
                    p -> new OutfitItem(o[0], OutfitItem.Slot.valueOf(o[1]), o[2], p)));
        }
    }

    /** One part item per shape. The material is stored on each stack. */
    public static final Map<String, DeferredItem<PartItem>> PARTS = new LinkedHashMap<>();

    static {
        for (PartShape shape : PartShapes.ALL) {
            PARTS.put(shape.id(), ITEMS.registerItem(shape.id(), p -> new PartItem(shape, p)));
        }
    }

    public static PartItem partItem(PartShape shape) {
        return PARTS.get(shape.id()).get();
    }

    private ModItems() {
    }
}
