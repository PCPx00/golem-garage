package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.golem.GolemData;
import com.xtrip.golemgarage.golem.GolemMaterial;
import com.xtrip.golemgarage.golem.GolemMaterials;
import com.xtrip.golemgarage.golem.PartShape;
import com.xtrip.golemgarage.golem.PartShapes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GolemGarage.MOD_ID);

    /** Ready-built golems shown in the main tab: one matched set per vanilla material. */
    private static final List<String> PRESET_MATERIALS = List.of("stone", "copper", "iron", "gold", "steel", "diamond", "netherite");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("modular_golems",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.golemgarage"))
                    .icon(() -> {
                        ItemStack s = GolemItem.stackFromTag(GolemData.preset(GolemMaterials.IRON,
                                "sentry_head", "plated_chest", "golem_fists", "stub_legs", "redstone_core"));
                        return s;
                    })
                    .displayItems((params, out) -> {
                        out.accept(ModItems.MANUAL.get());
                        out.accept(ModItems.GOLEM.get());
                        for (String id : PRESET_MATERIALS) {
                            GolemMaterial m = GolemMaterials.get(id);
                            out.accept(GolemItem.stackFromTag(GolemData.preset(m,
                                    "sentry_head", "plated_chest", "golem_fists", "stub_legs", "redstone_core")));
                        }
                        out.accept(GolemItem.stackFromTag(GolemData.preset(GolemMaterials.get("gold"),
                                "lantern_head", "cargo_barrel", "grabber_claws", "wheel_legs", "solar_core")));
                        out.accept(GolemItem.stackFromTag(GolemData.preset(GolemMaterials.get("netherite"),
                                "visor_head", "furnace_chest", "hammer_arms", "tread_legs", "furnace_core")));
                        out.accept(GolemItem.stackFromTag(GolemData.preset(GolemMaterials.get("diamond"),
                                "scope_head", "sealed_hold", "scythe_arms", "spider_legs", "lightning_core")));
                        for (DyeColor c : DyeColor.values()) {
                            ItemStack cub = new ItemStack(ModItems.CUB.get());
                            cub.set(ModDataComponents.COLOR.get(), c.getId());
                            out.accept(cub);
                        }
                        out.accept(ModItems.GARAGE_CONTROLLER.get());
                        out.accept(ModItems.LASER_EMITTER.get());
                        out.accept(ModItems.STEEL_EMITTER.get());
                        out.accept(ModItems.SIGNAL_PLATE.get());
                        out.accept(ModItems.GOLEM_DOCK.get());
                        out.accept(ModItems.MODULE_CASING.get());
                        ModItems.MODULES.values().forEach(m -> out.accept(m.get()));
                        ModItems.OUTFITS.values().forEach(o -> out.accept(o.get()));
                        out.accept(ModItems.GOLEM_LINKER.get());
                        out.accept(ModItems.BLUEPRINT.get());
                        out.accept(ModItems.REPAIR_KIT.get());
                        out.accept(ModItems.STEEL_INGOT.get());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PARTS = TABS.register("parts",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.golemgarage.parts"))
                    .withTabsBefore(MAIN.getKey())
                    .icon(() -> PartItem.create(PartShapes.get("golem_fists"), GolemMaterials.get("gold")))
                    .displayItems((params, out) -> {
                        for (PartShape shape : PartShapes.ALL) {
                            for (GolemMaterial m : GolemMaterials.CRAFTABLE) {
                                if (m.isAvailable()) {
                                    out.accept(PartItem.create(shape, m));
                                }
                            }
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
