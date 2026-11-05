package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xtrip.golemgarage.block.WindTurbineBlock;
import com.xtrip.golemgarage.power.WindTurbineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * The windmill above the turbine's base: a lattice mast, a gearbox with a tail vane, and eight rusty sheet-metal
 * blades that spin up and down with the output.
 */
public class WindTurbineRenderer implements BlockEntityRenderer<WindTurbineBlockEntity> {
    public WindTurbineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WindTurbineBlockEntity t, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-t.getBlockState().getValue(WindTurbineBlock.FACING).toYRot()));
        // Light from above the base, so the mast isn't drawn in the base's own shadow.
        int lit = t.getLevel() == null ? light : net.minecraft.client.renderer.LevelRenderer.getLightColor(t.getLevel(), t.getBlockPos().above(3));
        draw(pose, buffers, lit, Mth.lerp(partial, t.angleO, t.angle));
        pose.popPose();
    }

    /** Draws mast, gearbox and rotor, from the top of the base, the rotor facing +z. */
    static void draw(PoseStack pose, MultiBufferSource buffers, int light, float angle) {
        var vc = buffers.getBuffer(RenderType.entityCutoutNoCull(GarageSkin.TEX));
        var p = pose.last();
        int post = GarageSkin.SOOT, brace = GarageSkin.RUST;
        // Four legs leaning in from the base to the top.
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int k = 0; k < 4; k++) {
                    float y0 = 1 + k * 0.5f, f0 = 0.36f - k * 0.06f;
                    GarageSkin.box(vc, p, sx * f0 - 0.04f, y0, sz * f0 - 0.04f, sx * f0 + 0.04f, y0 + 0.5f,
                            sz * f0 + 0.04f, post, post, light);
                }
            }
        }
        // Cross braces on each level.
        for (int k = 0; k < 4; k++) {
            float y = 1.25f + k * 0.5f, f = 0.36f - k * 0.06f - 0.03f;
            GarageSkin.box(vc, p, -f, y, f - 0.02f, f, y + 0.04f, f + 0.02f, brace, brace, light);
            GarageSkin.box(vc, p, -f, y, -f - 0.02f, f, y + 0.04f, -f + 0.02f, brace, brace, light);
            GarageSkin.box(vc, p, f - 0.02f, y, -f, f + 0.02f, y + 0.04f, f, brace, brace, light);
            GarageSkin.box(vc, p, -f - 0.02f, y, -f, -f + 0.02f, y + 0.04f, f, brace, brace, light);
        }
        // Platform, gearbox and tail vane.
        GarageSkin.box(vc, p, -0.22f, 3.0f, -0.22f, 0.22f, 3.06f, 0.22f, GarageSkin.PLATES, GarageSkin.RUST, light);
        GarageSkin.box(vc, p, -0.13f, 3.06f, -0.3f, 0.13f, 3.32f, 0.3f, GarageSkin.RUST, GarageSkin.SOOT, light);
        GarageSkin.box(vc, p, -0.03f, 3.14f, -1.0f, 0.03f, 3.2f, -0.3f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        GarageSkin.box(vc, p, -0.012f, 2.98f, -1.35f, 0.012f, 3.5f, -0.9f, GarageSkin.OUTER_HAZARD, GarageSkin.SOOT, light);
        // Rotor.
        pose.pushPose();
        pose.translate(0, 3.19f, 0.34f);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        p = pose.last();
        GarageSkin.box(vc, p, -0.1f, -0.1f, -0.06f, 0.1f, 0.1f, 0.1f, CarModel.CHROME, CarModel.CHROME, light);
        for (int b = 0; b < 8; b++) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(b * 45f));
            pose.mulPose(Axis.YP.rotationDegrees(18f));
            var bp = pose.last();
            GarageSkin.box(vc, bp, -0.02f, 0.08f, -0.012f, 0.02f, 0.4f, 0.012f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            GarageSkin.box(vc, bp, -0.12f, 0.36f, -0.01f, 0.12f, 1.25f, 0.01f, CarModel.BLADE, CarModel.BLADE, light);
            pose.popPose();
        }
        // An outer ring holding the blade tips.
        for (int k = 0; k < 16; k++) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(k * 22.5f));
            GarageSkin.box(vc, pose.last(), -0.25f, 1.12f, -0.02f, 0.25f, 1.17f, 0.02f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(WindTurbineBlockEntity t) {
        return new AABB(t.getBlockPos()).inflate(1.6, 0, 1.6).expandTowards(0, 3.6, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(WindTurbineBlockEntity t) {
        return true;
    }
}
