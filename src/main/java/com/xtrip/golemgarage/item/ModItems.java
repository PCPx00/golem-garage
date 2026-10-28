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

    public static final DeferredItem<BlockItem> GARAGE_CONTROLLER =
            ITEMS.registerSimpleBlockItem("garage_controller", ModBlocks.GARAGE_CONTROLLER);
    public static final DeferredItem<BlockItem> LASER_EMITTER =
            ITEMS.registerSimpleBlockItem("laser_emitter", ModBlocks.LASER_EMITTER);
    public static final DeferredItem<BlockItem> STEEL_EMITTER =
            ITEMS.registerSimpleBlockItem("steel_emitter", ModBlocks.STEEL_EMITTER);
    public static final DeferredItem<BlockItem> SIGNAL_PLATE =
            ITEMS.registerSimpleBlockItem("signal_plate", ModBlocks.SIGNAL_PLATE);

    public static final DeferredItem<GolemItem> GOLEM = ITEMS.registerItem("golem", GolemItem::new);
    public static final DeferredItem<CubItem> CUB = ITEMS.registerItem("cub", CubItem::new);
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    /** Restores a quarter of every part's durability and health. */
    public static final DeferredItem<Item> REPAIR_KIT = ITEMS.registerSimpleItem("repair_kit");
    public static final DeferredItem<BlueprintItem> BLUEPRINT = ITEMS.registerItem("blueprint", BlueprintItem::new);
    public static final DeferredItem<GolemLinkerItem> GOLEM_LINKER = ITEMS.registerItem("golem_linker", GolemLinkerItem::new);
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
