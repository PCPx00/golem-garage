package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtrip.golemgarage.client.model.CubModel;
import com.xtrip.golemgarage.entity.CubEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.EnumMap;
import java.util.Map;

/** Renders a Cub in its dye color, with a softly glowing heart window. */
public class CubRenderer extends MobRenderer<CubEntity, CubModel> {
    @Override
    protected boolean shouldShowName(CubEntity entity) {
        return !GuiPreview.active && super.shouldShowName(entity);
    }

    private static final Map<DyeColor, ResourceLocation> SKINS = new EnumMap<>(DyeColor.class);
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath("golemgarage", "textures/entity/cub/glow.png");

    static {
        for (DyeColor c : DyeColor.values()) {
            SKINS.put(c, ResourceLocation.fromNamespaceAndPath("golemgarage", "textures/entity/cub/" + c.getName() + ".png"));
        }
    }

    public CubRenderer(EntityRendererProvider.Context context) {
        super(context, new CubModel(context.bakeLayer(CubModel.LAYER)), 0.35f);
        addLayer(new GlowLayer(this));
        addLayer(new OutfitLayer(this, context));
    }

    /** Draws each worn outfit piece on top of the Cub, following its pose. */
    static class OutfitLayer extends RenderLayer<CubEntity, CubModel> {
        private final Map<String, net.minecraft.client.model.geom.ModelPart> models = new java.util.HashMap<>();

        OutfitLayer(RenderLayerParent<CubEntity, CubModel> parent, EntityRendererProvider.Context context) {
            super(parent);
            for (var piece : com.xtrip.golemgarage.client.model.OutfitGeometry.PIECES) {
                models.put(piece.id(), context.bakeLayer(outfitLayer(piece.id())));
            }
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, CubEntity cub, float limbSwing,
                           float amount, float partialTick, float age, float yaw, float pitch) {
            if (cub.isInvisible()) {
                return;
            }
            int overlay = LivingEntityRenderer.getOverlayCoords(cub, 0f);
            for (String id : cub.outfitIds()) {
                var model = id.isEmpty() ? null : models.get(id);
                if (model == null) {
                    continue;
                }
                getParentModel().copyPoseTo(model);
                ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("golemgarage",
                        "textures/entity/cub_outfit/" + id + ".png");
                model.render(pose, buffers.getBuffer(RenderType.entityTranslucent(tex)), light, overlay);
            }
        }
    }

    public static net.minecraft.client.model.geom.ModelLayerLocation outfitLayer(String id) {
        return new net.minecraft.client.model.geom.ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath("golemgarage", "outfit_" + id), "main");
    }

    @Override
    public ResourceLocation getTextureLocation(CubEntity cub) {
        return SKINS.get(cub.getColor());
    }

    static class GlowLayer extends RenderLayer<CubEntity, CubModel> {
        GlowLayer(RenderLayerParent<CubEntity, CubModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, CubEntity cub, float limbSwing,
                           float amount, float partialTick, float age, float yaw, float pitch) {
            if (cub.isInvisible()) {
                return;
            }
            float pulse = 0.7f + 0.3f * (float) Math.sin(age * 0.15f);
            int v = (int) (255 * pulse);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), 0xF000F0,
                    LivingEntityRenderer.getOverlayCoords(cub, 0f), 0xFF000000 | (v << 16) | (v << 8) | v);
        }
    }
}
