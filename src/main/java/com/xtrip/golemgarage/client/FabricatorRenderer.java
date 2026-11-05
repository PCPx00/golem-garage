package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xtrip.golemgarage.block.entity.FabricatorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/**
 * The Fabricator's robot arm and work piece. Idle, the arm parks and the chosen part hovers as a faint hologram;
 * working, the arm sweeps round the part welding it, the part fades in from the hologram as it's built.
 */
public class FabricatorRenderer implements BlockEntityRenderer<FabricatorBlockEntity> {
    public FabricatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FabricatorBlockEntity fab, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (fab.getLevel() == null) {
            return;
        }
        float time = fab.getLevel().getGameTime() + partial;
        float p = fab.progress(partial);
        boolean working = fab.working();

        // The part on the bench: a slowly turning hologram, solid once it's being built.
        pose.pushPose();
        pose.translate(0.5, 1.05 + 0.03 * Mth.sin(time * 0.1f), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * (working ? 3f : 1.2f)));
        pose.scale(0.55f, 0.55f, 0.55f);
        Minecraft.getInstance().getItemRenderer().renderStatic(fab.preview(), ItemDisplayContext.FIXED,
                working ? light : 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers, fab.getLevel(), 0);
        pose.popPose();
        if (!working) {
            VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(0.5, 1.05, 0.5), 0.45f,
                    new float[]{0.35f, 0.9f, 1f}, 0.25f);
        }

        // The arm: a turret on the back corner, two jointed segments, a welding tip.
        float swing = working ? time * 0.18f : 0.6f;
        float reach = working ? 0.42f + 0.06f * Mth.sin(time * 0.37f) : 0.15f;
        pose.pushPose();
        pose.translate(0.82, 0.875, 0.82);
        pose.mulPose(Axis.YP.rotation((float) (Math.PI * 1.25 + (working ? 0.6 * Mth.sin(swing) : 0))));
        box(buffers, pose, -0.1f, 0, -0.1f, 0.1f, 0.12f, 0.1f, GarageSkin.SOOT, GarageSkin.RUST, light);
        pose.translate(0, 0.12, 0);
        pose.mulPose(Axis.ZP.rotation(-(0.5f + reach)));
        box(buffers, pose, -0.04f, 0, -0.04f, 0.04f, 0.5f, 0.04f, GarageSkin.RUST, GarageSkin.SOOT, light);
        pose.translate(0, 0.5, 0);
        pose.mulPose(Axis.ZP.rotation(-(1.4f - reach)));
        box(buffers, pose, -0.035f, 0, -0.035f, 0.035f, 0.42f, 0.035f, CarModel.CHROME, CarModel.CHROME, light);
        pose.translate(0, 0.42, 0);
        box(buffers, pose, -0.05f, 0, -0.05f, 0.05f, 0.07f, 0.05f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        if (working) {
            float flick = 0.6f + 0.4f * Mth.sin(time * 3.1f) * Mth.sin(time * 1.7f);
            VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(0, 0.1, 0), 0.22f,
                    new float[]{0.6f, 0.85f, 1f}, 0.9f * flick);
        }
        pose.popPose();

        // Progress strip along the front of the bench.
        if (working) {
            VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
            var m = pose.last().pose();
            float x1 = 0.1f + 0.8f * p;
            glow.addVertex(m, 0.1f, 0.3f, -0.002f).setColor(0.4f, 1f, 0.5f, 0.8f);
            glow.addVertex(m, x1, 0.3f, -0.002f).setColor(0.4f, 1f, 0.5f, 0.8f);
            glow.addVertex(m, x1, 0.36f, -0.002f).setColor(0.4f, 1f, 0.5f, 0.8f);
            glow.addVertex(m, 0.1f, 0.36f, -0.002f).setColor(0.4f, 1f, 0.5f, 0.8f);
        }
    }

    private static void box(MultiBufferSource buffers, PoseStack pose, float x0, float y0, float z0, float x1, float y1,
                            float z1, int side, int top, int light) {
        GarageSkin.box(buffers.getBuffer(RenderType.entityCutoutNoCull(GarageSkin.TEX)), pose.last(),
                x0, y0, z0, x1, y1, z1, side, top, light);
    }
}
