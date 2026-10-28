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
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalysts(GarageRepairCategory.TYPE, ModItems.GARAGE_CONTROLLER.get(),
                ModItems.LASER_EMITTER.get(), ModItems.STEEL_EMITTER.get());
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        reg.addRecipes(GarageRepairCategory.TYPE, repairExamples());

        info(reg, ModItems.MANUAL.get(), "manual");
        info(reg, ModItems.GOLEM.get(), "golem");
        info(reg, ModItems.CUB.get(), "cub");
        info(reg, ModItems.GARAGE_CONTROLLER.get(), "garage");
        info(reg, ModItems.LASER_EMITTER.get(), "garage");
        info(reg, ModItems.STEEL_EMITTER.get(), "garage");
        info(reg, ModItems.SIGNAL_PLATE.get(), "signal_plate");
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
