package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtrip.golemgarage.client.model.GolemModel;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.golem.PartSlot;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws a modular golem. The base model pass is skipped; instead a layer draws each slot with its
 * own material texture, then cracks for worn parts, the core-colored glow, and vines.
 */
public class GolemRenderer extends MobRenderer<GolemEntity, GolemModel> {
    @Override
    protected boolean shouldShowName(GolemEntity entity) {
        return !GuiPreview.active && super.shouldShowName(entity);
    }

    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static final ResourceLocation GLOW = tex("glow");
    private static final ResourceLocation VINES = tex("vines");
    private static final ResourceLocation PAINT = tex("paint");
    private static final ResourceLocation STATUS = ResourceLocation.fromNamespaceAndPath("golemgarage",
            "textures/gui/status_icons.png");
    private static final ResourceLocation[] CRACKS = {tex("cracks_1"), tex("cracks_2"), tex("cracks_3")};

    public GolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel(context.bakeLayer(GolemModel.LAYER)), 0.9f);
        addLayer(new PartsLayer(this));
    }

    static ResourceLocation tex(String name) {
        return TEXTURES.computeIfAbsent(name,
                n -> ResourceLocation.fromNamespaceAndPath("golemgarage", "textures/entity/golem/" + n + ".png"));
    }

    @Override
    public ResourceLocation getTextureLocation(GolemEntity golem) {
        return tex("frame");
    }

    /** The base pass is replaced by {@link PartsLayer}. */
    @Override
    protected RenderType getRenderType(GolemEntity golem, boolean visible, boolean translucent, boolean glowing) {
        return null;
    }

    @Override
    public void render(GolemEntity golem, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        model.choose(golem);
        super.render(golem, yaw, partialTick, pose, buffers, light);
        if (com.xtrip.golemgarage.Config.SHOW_STATUS_ICONS.get()) {
            renderStatusIcon(golem, partialTick, pose, buffers);
        }
    }

    /** Which icon to float over the golem: 0 stopped, 1 stuck, 2 broken part, 3 no power, 4 low power; -1 none. */
    static int statusIcon(GolemEntity golem) {
        String status = golem.readSyncedStatus();
        if (golem.isBreakingDown()) {
            return 2;
        }
        if (com.xtrip.golemgarage.golem.GolemText.isProblem(status)) {
            return status.contains("stopped") || status.startsWith("Stopped") ? 0 : 1;
        }
        for (PartSlot slot : PartSlot.values()) {
            PartInstance p = golem.getPart(slot);
            if (p != null && p.isBroken()) {
                return 2;
            }
        }
        int cap = golem.readSyncedCapacity();
        if (cap > 0) {
            int power = golem.readSyncedPower();
            if (power <= 0) {
                return 3;
            }
            if (power * 100 / cap < 15) {
                return 4;
            }
        }
        return -1;
    }

    private void renderStatusIcon(GolemEntity golem, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        int icon = statusIcon(golem);
        if (icon < 0 || golem.isInvisible() || GuiPreview.active) {
            return;
        }
        float age = golem.tickCount + partialTick;
        pose.pushPose();
        pose.translate(0, golem.getBbHeight() + 0.55 + Mth.sin(age * 0.12f) * 0.06, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float sc = 0.5f;
        pose.scale(sc, sc, sc);
        var vc = buffers.getBuffer(RenderType.text(STATUS));
        PoseStack.Pose last = pose.last();
        float u0 = icon * 16 / 80f;
        float u1 = (icon * 16 + 16) / 80f;
        int light = 0xF000F0;
        vc.addVertex(last, -0.5f, -0.5f, 0).setColor(255, 255, 255, 255).setUv(u1, 1).setLight(light);
        vc.addVertex(last, 0.5f, -0.5f, 0).setColor(255, 255, 255, 255).setUv(u0, 1).setLight(light);
        vc.addVertex(last, 0.5f, 0.5f, 0).setColor(255, 255, 255, 255).setUv(u0, 0).setLight(light);
        vc.addVertex(last, -0.5f, 0.5f, 0).setColor(255, 255, 255, 255).setUv(u1, 0).setLight(light);
        pose.popPose();
    }

    static class PartsLayer extends RenderLayer<GolemEntity, GolemModel> {
        PartsLayer(RenderLayerParent<GolemEntity, GolemModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, GolemEntity golem, float limbSwing,
                           float amount, float partialTick, float age, float yaw, float pitch) {
            if (golem.isInvisible()) {
                return;
            }
            GolemModel model = getParentModel();
            int overlay = LivingEntityRenderer.getOverlayCoords(golem, 0f);

            // One pass per slot, each in its own material.
            for (PartSlot slot : PartSlot.values()) {
                if (!model.shows(slot)) {
                    continue;
                }
                PartInstance part = golem.getPart(slot);
                String mat = part == null ? "frame" : part.visualMaterialId();
                int color = part != null && part.isBroken() ? 0xFF8A8A8A : 0xFFFFFFFF;
                model.showOnly(slot);
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex(mat))), light, overlay, color);
                if (part != null) {
                    float cond = part.condition();
                    int level = part.isBroken() || cond < 0.25f ? 2 : cond < 0.5f ? 1 : cond < 0.75f ? 0 : -1;
                    if (level >= 0) {
                        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CRACKS[level])),
                                light, overlay, 0xFFFFFFFF);
                    }
                }
            }

            model.showOnly(null);
            if (golem.getPaint() >= 0) {
                int dye = net.minecraft.world.item.DyeColor.byId(golem.getPaint()).getTextureDiffuseColor();
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(PAINT)), light, overlay,
                        0xFF000000 | dye);
            }
            if (golem.hasVines()) {
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(VINES)), light, overlay, 0xFFFFFFFF);
            }

            // Glow: eyes, core windows and lanterns light up in the core's color.
            int rgb = golem.stats().glowColor;
            float bright = 1f;
            if (golem.readSyncedCapacity() > 0 && golem.readSyncedPower() <= 0) {
                bright = 0.25f + 0.15f * Mth.sin(age * 0.4f);
            }
            int glow = FastColor.ARGB32.color(255,
                    (int) (FastColor.ARGB32.red(rgb) * bright),
                    (int) (FastColor.ARGB32.green(rgb) * bright),
                    (int) (FastColor.ARGB32.blue(rgb) * bright));
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), 0xF000F0, overlay, glow);
        }
    }
}
