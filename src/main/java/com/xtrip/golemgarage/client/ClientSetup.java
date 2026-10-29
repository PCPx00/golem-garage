package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.block.entity.ModBlockEntities;
import com.xtrip.golemgarage.client.model.CubModel;
import com.xtrip.golemgarage.client.model.GolemModel;
import com.xtrip.golemgarage.entity.ModEntities;
import com.xtrip.golemgarage.golem.GolemMaterials;
import com.xtrip.golemgarage.item.CubItem;
import com.xtrip.golemgarage.item.GolemItem;
import com.xtrip.golemgarage.item.ModDataComponents;
import com.xtrip.golemgarage.item.ModItems;
import com.xtrip.golemgarage.item.PartItem;
import com.xtrip.golemgarage.particle.ModParticles;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredItem;

@EventBusSubscriber(modid = GolemGarage.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GolemModel.LAYER, GolemModel::createBodyLayer);
        event.registerLayerDefinition(CubModel.LAYER, CubModel::createBodyLayer);
        for (var piece : com.xtrip.golemgarage.client.model.OutfitGeometry.PIECES) {
            event.registerLayerDefinition(CubRenderer.outfitLayer(piece.id()), piece.layer());
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GOLEM.get(), GolemRenderer::new);
        event.registerEntityRenderer(ModEntities.CUB.get(), CubRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GARAGE_CONTROLLER.get(), GarageControllerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GARAGE_DOOR.get(), GarageDoorRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.xtrip.golemgarage.menu.ModMenus.CUB.get(), CubScreen::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SLEEP_Z.get(), SleepParticle.Provider::new);
    }

    /** Parts and golems made of a datapack material show the borrowed icon in that material's tint. */
    @SubscribeEvent
    public static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        for (DeferredItem<PartItem> part : ModItems.PARTS.values()) {
            event.register((stack, layer) -> layer == 0 ? GolemMaterials.tintOf(
                    stack.getOrDefault(ModDataComponents.MATERIAL.get(), "iron")) : 0xFFFFFFFF, part.get());
        }
        event.register((stack, layer) -> layer == 0 ? GolemMaterials.tintOf(GolemItem.iconMaterial(stack)) : 0xFFFFFFFF,
                ModItems.GOLEM.get());
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation material = ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "material");
            for (DeferredItem<PartItem> part : ModItems.PARTS.values()) {
                ItemProperties.register(part.get(), material, (stack, level, entity, seed) ->
                        GolemMaterials.craftableIndex(stack.getOrDefault(ModDataComponents.MATERIAL.get(), "iron")) * 0.01f + 0.005f);
            }
            ItemProperties.register(ModItems.GOLEM.get(), material, (stack, level, entity, seed) ->
                    GolemMaterials.allIndex(GolemItem.iconMaterial(stack)) * 0.01f + 0.005f);
            ItemProperties.register(ModItems.GOLEM.get(), ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "broken"),
                    (stack, level, entity, seed) -> GolemItem.isBroken(stack) ? 1f : 0f);
            ItemProperties.register(ModItems.CUB.get(), ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "color"),
                    (stack, level, entity, seed) -> (CubItem.colorId(stack) + 1) * 0.01f + 0.005f);
        });
    }

    private ClientSetup() {
    }
}
