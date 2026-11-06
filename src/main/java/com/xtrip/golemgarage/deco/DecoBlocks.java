package com.xtrip.golemgarage.deco;

import com.xtrip.golemgarage.block.ModBlocks;
import com.xtrip.golemgarage.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Garage decorations (tyre stacks, oil drums, a workbench, a chain hoist, neon signs), trophies (the Junk Titan's
 * head, medals), the rig gadgets from 0.27 and the jerry can.
 */
public final class DecoBlocks {
    public static final DeferredBlock<TyreStackBlock> TYRE_STACK = ModBlocks.BLOCKS.register("tyre_stack",
            () -> new TyreStackBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(0.8f).sound(SoundType.WOOL).noOcclusion()));
    public static final Map<String, DeferredBlock<OilDrumBlock>> OIL_DRUMS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<NeonSignBlock>> NEON_SIGNS = new LinkedHashMap<>();

    static {
        for (String c : new String[]{"red", "blue", "rust"}) {
            OIL_DRUMS.put(c, ModBlocks.BLOCKS.register("oil_drum_" + c,
                    () -> new OilDrumBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                            .strength(1.5f, 4f).sound(SoundType.METAL).noOcclusion()
                            .lightLevel(s -> s.getValue(OilDrumBlock.LIT) ? 14 : 0))));
        }
        for (String w : new String[]{"open", "garage", "scrap"}) {
            NEON_SIGNS.put(w, ModBlocks.BLOCKS.register("neon_" + w,
                    () -> new NeonSignBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
                            .strength(0.6f).sound(SoundType.GLASS).noOcclusion().noCollission()
                            .lightLevel(s -> s.getValue(NeonSignBlock.LIT) ? 10 : 0))));
        }
    }

    public static final DeferredBlock<WorkbenchBlock> WORKBENCH = ModBlocks.BLOCKS.register("workbench",
            () -> new WorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.5f, 6f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<DecoBlock> CHAIN_HOIST = ModBlocks.BLOCKS.register("chain_hoist",
            () -> new DecoBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 6f).sound(SoundType.CHAIN).noOcclusion(), 4, 0, 4, 12, 16, 12));
    public static final DeferredBlock<DecoBlock> TITAN_HEAD = ModBlocks.BLOCKS.register("titan_head",
            () -> new DecoBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(3f, 9f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().lightLevel(s -> 5),
                    1, 0, 1, 15, 15, 15));
    public static final DeferredBlock<SpikeStripBlock> SPIKE_STRIP = ModBlocks.BLOCKS.register("spike_strip",
            () -> new SpikeStripBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(0.5f).sound(SoundType.CHAIN).noOcclusion().noLootTable()));

    public static final DeferredItem<BlockItem> TYRE_STACK_ITEM = ModItems.extraBlock("tyre_stack", TYRE_STACK);
    public static final Map<String, DeferredItem<BlockItem>> OIL_DRUM_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> NEON_ITEMS = new LinkedHashMap<>();

    static {
        OIL_DRUMS.forEach((c, b) -> OIL_DRUM_ITEMS.put(c, ModItems.extraBlock("oil_drum_" + c, b)));
        NEON_SIGNS.forEach((w, b) -> NEON_ITEMS.put(w, ModItems.extraBlock("neon_" + w, b)));
    }

    public static final DeferredItem<BlockItem> WORKBENCH_ITEM = ModItems.extraBlock("workbench", WORKBENCH);
    public static final DeferredItem<BlockItem> CHAIN_HOIST_ITEM = ModItems.extraBlock("chain_hoist", CHAIN_HOIST);
    public static final DeferredItem<BlockItem> TITAN_HEAD_ITEM = ModItems.addExtra(ModItems.ITEMS.registerSimpleBlockItem(
            "titan_head", TITAN_HEAD, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> SPIKE_STRIP_ITEM = ModItems.extraBlock("spike_strip", SPIKE_STRIP);

    public static final DeferredItem<Item> RIG_HARPOON = ModItems.addExtra(ModItems.ITEMS.registerSimpleItem("rig_harpoon"));
    public static final DeferredItem<Item> RIG_SMOKE = ModItems.addExtra(ModItems.ITEMS.registerSimpleItem("rig_smoke"));
    public static final DeferredItem<Item> RIG_SPIKES = ModItems.addExtra(ModItems.ITEMS.registerSimpleItem("rig_spikes"));
    public static final DeferredItem<Item> RIG_RACK = ModItems.addExtra(ModItems.ITEMS.registerSimpleItem("rig_rack"));
    public static final DeferredItem<JerryCanItem> JERRY_CAN = ModItems.addExtra(ModItems.ITEMS.registerItem("jerry_can",
            JerryCanItem::new, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<MedalItem> LEGEND_MEDAL = ModItems.addExtra(ModItems.ITEMS.registerItem("legend_medal",
            MedalItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<MedalItem> ARENA_MEDAL = ModItems.addExtra(ModItems.ITEMS.registerItem("arena_medal",
            MedalItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    private DecoBlocks() {
    }

    /** Loads the class so everything above is registered. */
    public static void init() {
    }
}
