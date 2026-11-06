package com.xtrip.golemgarage.compat.jei;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.golem.GolemData;
import com.xtrip.golemgarage.golem.GolemMaterial;
import com.xtrip.golemgarage.golem.GolemMaterials;
import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.golem.PartSlot;
import com.xtrip.golemgarage.item.CubItem;
import com.xtrip.golemgarage.item.GolemItem;
import com.xtrip.golemgarage.item.ModItems;
import com.xtrip.golemgarage.item.PartItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** JEI support: tells apart part materials, golem builds and cub colours, adds info pages and the Garage repair recipe. */
@JeiPlugin
public class GolemGarageJei implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration reg) {
        ISubtypeInterpreter<ItemStack> material = new Simple(s -> PartItem.materialId(s));
        ModItems.PARTS.values().forEach(p -> reg.registerSubtypeInterpreter(p.get(), material));
        reg.registerSubtypeInterpreter(ModItems.CUB.get(), new Simple(s -> "c" + CubItem.colorId(s)));
        reg.registerSubtypeInterpreter(ModItems.GOLEM.get(), new Simple(GolemGarageJei::golemSignature));
    }

    /** A golem's subtype is its build (shape + material per slot) and whether it's broken, never its wear. */
    static String golemSignature(ItemStack stack) {
        CompoundTag tag = GolemItem.data(stack);
        String parts = GolemData.readParts(tag).values().stream()
                .map(p -> p.shape().id() + "=" + p.materialId())
                .collect(Collectors.joining(","));
        return (tag.getBoolean("Broken") ? "broken:" : "") + parts;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new GarageRepairCategory(reg.getJeiHelpers().getGuiHelper()));
        reg.addRecipeCategories(new FabricatorCategory(reg.getJeiHelpers().getGuiHelper()));
        reg.addRecipeCategories(new BurnerCategory(reg.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalysts(GarageRepairCategory.TYPE, ModItems.GARAGE_CONTROLLER.get(),
                ModItems.LASER_EMITTER.get(), ModItems.STEEL_EMITTER.get());
        reg.addRecipeCatalysts(FabricatorCategory.TYPE, ModItems.FABRICATOR.get());
        reg.addRecipeCatalysts(BurnerCategory.TYPE, com.xtrip.golemgarage.power.PowerBlocks.BURNER_ITEM.get());
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        reg.addRecipes(GarageRepairCategory.TYPE, repairExamples());
        reg.addRecipes(FabricatorCategory.TYPE, fabricatorWelds());
        reg.addRecipes(BurnerCategory.TYPE, burnerFuels());

        info(reg, ModItems.MANUAL.get(), "manual");
        info(reg, ModItems.GOLEM.get(), "golem");
        info(reg, ModItems.CUB.get(), "cub");
        info(reg, ModItems.GARAGE_CONTROLLER.get(), "garage");
        info(reg, ModItems.LASER_EMITTER.get(), "garage");
        info(reg, ModItems.STEEL_EMITTER.get(), "garage");
        info(reg, ModItems.GARAGE_DOOR.get(), "garage_door");
        info(reg, ModItems.RIG.get(), "rig");
        info(reg, ModItems.RIG_RAM.get(), "rig");
        info(reg, ModItems.RIG_TURRET.get(), "rig");
        info(reg, ModItems.RIG_PLATING.get(), "rig");
        info(reg, ModItems.SCRAP.get(), "scrap");
        info(reg, ModItems.RULE_CARD.get(), "rule_card");
        info(reg, ModItems.WAR_HORN.get(), "war_horn");
        info(reg, ModItems.FABRICATOR.get(), "fabricator");
        info(reg, ModItems.CUB_BED.get(), "cub_bed");
        info(reg, ModItems.GARAGE_RADIO.get(), "garage_radio");
        info(reg, com.xtrip.golemgarage.power.PowerBlocks.SOLAR_PANEL_ITEM.get(), "solar_panel");
        info(reg, com.xtrip.golemgarage.power.PowerBlocks.WIND_TURBINE_ITEM.get(), "wind_turbine");
        info(reg, com.xtrip.golemgarage.power.PowerBlocks.BURNER_ITEM.get(), "burner_generator");
        info(reg, com.xtrip.golemgarage.power.PowerBlocks.BATTERY_ITEM.get(), "battery");
        info(reg, com.xtrip.golemgarage.power.PowerBlocks.CUB_CHARGER_ITEM.get(), "cub_charger");
        info(reg, ModItems.RIG_INTERCEPTOR.get(), "rig_interceptor");
        info(reg, ModItems.ARENA_BELL.get(), "arena_bell");
        {
            for (var g : List.of(com.xtrip.golemgarage.deco.DecoBlocks.RIG_HARPOON, com.xtrip.golemgarage.deco.DecoBlocks.RIG_SMOKE,
                    com.xtrip.golemgarage.deco.DecoBlocks.RIG_SPIKES, com.xtrip.golemgarage.deco.DecoBlocks.RIG_RACK)) {
                info(reg, g.get(), "rig_gadgets");
            }
            info(reg, com.xtrip.golemgarage.deco.DecoBlocks.JERRY_CAN.get(), "jerry_can");
            info(reg, com.xtrip.golemgarage.deco.DecoBlocks.TITAN_HEAD_ITEM.get(), "titan_head");
            info(reg, com.xtrip.golemgarage.deco.DecoBlocks.LEGEND_MEDAL.get(), "medals");
            info(reg, com.xtrip.golemgarage.deco.DecoBlocks.ARENA_MEDAL.get(), "medals");
            info(reg, com.xtrip.golemgarage.deco.DecoBlocks.TYRE_STACK_ITEM.get(), "tyre_stack");
            com.xtrip.golemgarage.deco.DecoBlocks.OIL_DRUM_ITEMS.values().forEach(i -> info(reg, i.get(), "oil_drum"));
            com.xtrip.golemgarage.deco.DecoBlocks.NEON_ITEMS.values().forEach(i -> info(reg, i.get(), "neon"));
        }
        info(reg, ModItems.GARAGE_BLUEPRINT.get(), "garage_blueprint");
        info(reg, ModItems.RIG_JUGGERNAUT.get(), "rig_juggernaut");
        info(reg, ModItems.RIG_SKIMMER.get(), "rig_skimmer");
        info(reg, ModItems.GOLEM_TABLET.get(), "golem_tablet");
        ModItems.SPRAY_CANS.values().forEach(c -> info(reg, c.get(), "spray_can"));
        info(reg, ModItems.GARAGE_WINDOW.get(), "garage_window");
        info(reg, ModItems.SIGNAL_PLATE.get(), "signal_plate");
        info(reg, ModItems.GOLEM_DOCK.get(), "dock");
        info(reg, ModItems.GOLEM_LINKER.get(), "linker");
        info(reg, ModItems.REPAIR_KIT.get(), "repair_kit");
        info(reg, ModItems.BLUEPRINT.get(), "blueprint");
        info(reg, ModItems.STEEL_INGOT.get(), "steel_ingot");
        info(reg, ModItems.MODULE_CASING.get(), "module_casing");
        ModItems.MODULES.values().forEach(m -> info(reg, m.get(), "module"));
        ModItems.OUTFITS.values().forEach(o -> info(reg, o.get(), "outfit"));
        ModItems.PARTS.values().forEach(p -> info(reg, p.get(), "part"));
    }

    private static void info(IRecipeRegistration reg, ItemLike item, String key) {
        reg.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK,
                Component.translatable("jei." + GolemGarage.MOD_ID + ".info." + key));
    }

    /** One worn-out → repaired example per preset material, priced like the Garage prices it. */
    private static List<GarageRepairCategory.Repair> repairExamples() {
        int per = 1;
        try {
            per = Math.max(0, com.xtrip.golemgarage.Config.REPAIR_COST_PER_PART.get());
        } catch (IllegalStateException notLoadedYet) {
            // Server config isn't synced before joining a world; JEI reloads on join anyway.
        }
        List<GarageRepairCategory.Repair> out = new ArrayList<>();
        for (String id : List.of("stone", "copper", "iron", "gold", "steel", "diamond", "netherite")) {
            GolemMaterial m = GolemMaterials.get(id);
            if (m == null || m.tagKey() == null) {
                continue;
            }
            CompoundTag fixed = GolemData.preset(m, "sentry_head", "plated_chest", "golem_fists", "stub_legs", "redstone_core");
            Map<PartSlot, PartInstance> worn = new EnumMap<>(PartSlot.class);
            GolemData.readParts(fixed).forEach((slot, p) ->
                    worn.put(slot, new PartInstance(p.shape(), p.materialId(), p.maxDurability(), 0, false)));
            CompoundTag broken = fixed.copy();
            broken.put("Parts", GolemData.writeParts(worn));
            broken.putBoolean("Broken", true);
            int count = Math.max(1, worn.size() * per);
            List<ItemStack> mats = Arrays.stream(Ingredient.of(m.tagKey()).getItems())
                    .map(s -> s.copyWithCount(count)).toList();
            if (mats.isEmpty()) {
                continue;
            }
            out.add(new GarageRepairCategory.Repair(GolemItem.stackFromTag(broken), per == 0 ? List.of() : mats,
                    GolemItem.stackFromTag(fixed)));
        }
        return out;
    }

    /** Every part shape in every craftable material, at the Fabricator's price. */
    private static List<FabricatorCategory.Weld> fabricatorWelds() {
        List<FabricatorCategory.Weld> out = new ArrayList<>();
        for (GolemMaterial m : GolemMaterials.CRAFTABLE) {
            if (m.tagKey() == null || m.isBlocked()) {
                continue;
            }
            List<ItemStack> raw = Arrays.asList(Ingredient.of(m.tagKey()).getItems());
            if (raw.isEmpty()) {
                continue;
            }
            for (com.xtrip.golemgarage.golem.PartShape shape : com.xtrip.golemgarage.golem.PartShapes.ALL) {
                int n = com.xtrip.golemgarage.block.entity.FabricatorBlockEntity.cost(shape.slot());
                out.add(new FabricatorCategory.Weld(raw.stream().map(st -> st.copyWithCount(n)).toList(),
                        PartItem.create(shape, m)));
            }
        }
        return out;
    }

    /** Burner fuels grouped by burn time, plus lava from the tank. */
    private static List<BurnerCategory.Fuel> burnerFuels() {
        Map<Integer, List<ItemStack>> byTime = new java.util.TreeMap<>(java.util.Comparator.reverseOrder());
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            ItemStack st = new ItemStack(item);
            if (st.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
                continue;
            }
            int t = st.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
            if (t > 0) {
                byTime.computeIfAbsent(t, k -> new ArrayList<>()).add(st);
            }
        }
        List<BurnerCategory.Fuel> out = new ArrayList<>();
        int rate = com.xtrip.golemgarage.power.BurnerGeneratorBlockEntity.ITEM_RATE;
        int lava = com.xtrip.golemgarage.power.BurnerGeneratorBlockEntity.LAVA_RATE;
        int lavaSeconds = 1000 / com.xtrip.golemgarage.power.BurnerGeneratorBlockEntity.LAVA_SIP
                * com.xtrip.golemgarage.power.BurnerGeneratorBlockEntity.LAVA_TICKS / 20;
        out.add(new BurnerCategory.Fuel(List.of(new ItemStack(net.minecraft.world.item.Items.LAVA_BUCKET)),
                (long) lava * lavaSeconds * 20, lava, lavaSeconds));
        byTime.forEach((t, items) -> out.add(new BurnerCategory.Fuel(items, (long) t * rate, rate, Math.max(1, t / 20))));
        return out;
    }

    private record Simple(java.util.function.Function<ItemStack, String> fn) implements ISubtypeInterpreter<ItemStack> {
        @Override
        public @Nullable Object getSubtypeData(ItemStack stack, UidContext context) {
            return fn.apply(stack);
        }

        @Override
        public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
            return fn.apply(stack);
        }
    }
}
