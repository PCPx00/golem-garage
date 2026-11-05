package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The wasteland car, drawn from boxes of the Garage skin. Shared by the car idling in a finished Garage and the
 * drivable Wasteland Rig. Car space: x forward, y up from the ground, z across; about 2.5 long and 1.2 wide.
 */
public final class CarModel {
    static final int CAR_BODY = 23, CAR_GLASS = 24, GEAR = 25, CHROME = 27, GRILLE = 28;

    /** What to draw: the upgrades fitted, how far the wheels have turned and where the turret points. */
    public record Options(boolean ram, boolean turret, boolean plating, float wheelAngle, float turretYaw,
                          boolean engine, boolean backfire) {
    }

    private CarModel() {
    }

    private static void box(MultiBufferSource buffers, PoseStack pose, float x0, float y0, float z0, float x1, float y1,
                            float z1, int side, int top, int light) {
        GarageSkin.box(buffers.getBuffer(RenderType.entityCutoutNoCull(GarageSkin.TEX)), pose.last(),
                x0, y0, z0, x1, y1, z1, side, top, light);
    }

    public static void render(BlockRenderDispatcher blocks, PoseStack pose, MultiBufferSource buffers, int light,
                              float time, Options o) {
        // Wheels roll with the ground; the body shakes on its springs while the engine runs.
        wheel(buffers, pose, 0.84f, 0.27f, 0.5f, 0.2f, o.wheelAngle(), light);
        wheel(buffers, pose, 0.84f, 0.27f, -0.5f, 0.2f, o.wheelAngle(), light);
        wheel(buffers, pose, -0.82f, 0.33f, 0.5f, 0.26f, o.wheelAngle() * 0.82f, light);
        wheel(buffers, pose, -0.82f, 0.33f, -0.5f, 0.26f, o.wheelAngle() * 0.82f, light);

        pose.pushPose();
        float shake = o.engine() ? 1f : 0f;
        pose.translate(0, shake * (0.01f * Mth.sin(time * 2.7f) + 0.005f * Mth.sin(time * 5.3f)), 0);
        pose.mulPose(Axis.XP.rotationDegrees(shake * 0.5f * Mth.sin(time * 2.1f)));

        box(buffers, pose, -1.25f, 0.24f, -0.46f, 1.25f, 0.42f, 0.46f, GarageSkin.RUST, GarageSkin.RUST, light);
        box(buffers, pose, -1.18f, 0.4f, -0.5f, 1.12f, 0.72f, 0.5f, CAR_BODY, CAR_BODY, light);
        box(buffers, pose, 0.24f, 0.72f, -0.46f, 1.12f, 0.79f, 0.46f, CAR_BODY, CAR_BODY, light);
        box(buffers, pose, -0.72f, 0.72f, -0.43f, 0.22f, 1.1f, 0.43f, CAR_GLASS, CAR_BODY, light);
        box(buffers, pose, -0.76f, 1.1f, -0.46f, 0.26f, 1.16f, 0.46f, CAR_BODY, CAR_BODY, light);
        box(buffers, pose, -1.18f, 0.72f, -0.47f, -0.72f, 0.8f, 0.47f, GarageSkin.RUST, GarageSkin.RUST, light);
        // Roll cage over the cabin.
        for (float z : new float[]{-0.45f, 0.41f}) {
            box(buffers, pose, -0.74f, 1.16f, z, 0.24f, 1.22f, z + 0.04f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        }
        box(buffers, pose, 0.2f, 1.16f, -0.45f, 0.24f, 1.22f, 0.45f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        // Grille, and a spiked ram on the front (or a plain bumper).
        box(buffers, pose, 1.12f, 0.43f, -0.4f, 1.17f, 0.7f, 0.4f, GRILLE, CHROME, light);
        if (!o.ram()) {
            box(buffers, pose, 1.15f, 0.32f, -0.48f, 1.22f, 0.42f, 0.48f, CHROME, CHROME, light);
        } else {
        box(buffers, pose, 1.17f, 0.3f, -0.56f, 1.28f, 0.45f, 0.56f, GarageSkin.RUST, GarageSkin.RUST, light);
        for (float z : new float[]{-0.4f, 0f, 0.4f}) {
            pose.pushPose();
            pose.translate(1.27f, 0.375f, z);
            pose.mulPose(Axis.ZP.rotationDegrees(-90));
            pose.scale(0.4f, 0.5f, 0.4f);
            pose.translate(-0.5, 0, -0.5);
            blocks.renderSingleBlock(Blocks.POINTED_DRIPSTONE.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        }
        // The supercharger sticking out of the bonnet, its belt pulley spinning.
        box(buffers, pose, 0.5f, 0.79f, -0.16f, 0.86f, 1.0f, 0.16f, CHROME, CHROME, light);
        box(buffers, pose, 0.56f, 1.0f, -0.12f, 0.8f, 1.13f, 0.12f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        for (int k = 0; k < 2; k++) {
            pose.pushPose();
            pose.translate(0.88f, 0.89f, 0);
            pose.mulPose(Axis.XP.rotationDegrees(shake * time * 24f + k * 45f));
            box(buffers, pose, -0.02f, -0.08f, -0.08f, 0.02f, 0.08f, 0.08f, GEAR, CHROME, light);
            pose.popPose();
        }
        // Side pipes and twin stacks behind the cabin (their smoke comes from the controller's client tick).
        for (int side = -1; side <= 1; side += 2) {
            float z0 = side > 0 ? 0.5f : -0.6f;
            box(buffers, pose, -0.34f, 0.3f, z0, 0.7f, 0.38f, z0 + 0.1f, CHROME, CHROME, light);
            float sz = 0.36f * side;
            box(buffers, pose, -0.68f, 0.72f, sz - 0.05f, -0.56f, 1.48f, sz + 0.05f, CHROME, GarageSkin.SOOT, light);
        }
        // Spare tyre and a jerry can on the back deck.
        box(buffers, pose, -1.12f, 0.8f, -0.3f, -0.78f, 0.9f, 0.04f, GarageSkin.TYRE, GarageSkin.TYRE_TOP, light);
        box(buffers, pose, -1.1f, 0.8f, 0.12f, -0.86f, 1.08f, 0.26f, GarageSkin.JERRY, GarageSkin.JERRY, light);
        // Headlights, flickering with the engine.
        box(buffers, pose, 1.11f, 0.56f, 0.24f, 1.16f, 0.66f, 0.38f, CHROME, CHROME, light);
        box(buffers, pose, 1.11f, 0.56f, -0.38f, 1.16f, 0.66f, -0.24f, CHROME, CHROME, light);
        // Armour plates bolted over the doors and a grille over the windscreen.
        if (o.plating()) {
            for (int side = -1; side <= 1; side += 2) {
                float z0 = side > 0 ? 0.5f : -0.54f;
                box(buffers, pose, -0.9f, 0.42f, z0, 0.9f, 0.74f, z0 + 0.04f, GarageSkin.PLATES, GarageSkin.RUST, light);
                box(buffers, pose, -0.7f, 0.76f, z0 * 0.92f, 0.2f, 1.06f, z0 * 0.92f + 0.03f, GRILLE, GRILLE, light);
            }
            box(buffers, pose, 0.22f, 0.76f, -0.42f, 0.25f, 1.08f, 0.42f, GRILLE, GRILLE, light);
        }
        // A twin-barrel turret on the cab roof.
        if (o.turret()) {
            box(buffers, pose, -0.5f, 1.22f, -0.2f, -0.1f, 1.3f, 0.2f, GarageSkin.SOOT, GarageSkin.RUST, light);
            pose.pushPose();
            pose.translate(-0.3f, 1.3f, 0);
            pose.mulPose(Axis.YP.rotationDegrees(o.turretYaw()));
            box(buffers, pose, -0.16f, 0f, -0.14f, 0.12f, 0.18f, 0.14f, GarageSkin.RUST, GarageSkin.SOOT, light);
            for (float z : new float[]{-0.07f, 0.04f}) {
                box(buffers, pose, 0.1f, 0.07f, z, 0.62f, 0.11f, z + 0.03f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            }
            box(buffers, pose, -0.14f, 0.18f, -0.1f, 0.04f, 0.24f, 0.1f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            pose.popPose();
        }
        float flick = o.engine() ? 0.75f + 0.15f * Mth.sin(time * 1.7f) + 0.1f * Mth.sin(time * 4.9f) : 0f;
        VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
        for (float z : new float[]{0.31f, -0.31f}) {
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(1.18, 0.61, z), 0.35f,
                    new float[]{1f, 0.9f, 0.6f}, 0.55f * flick);
        }
        // Backfire flashes from the side pipes.
        if (o.backfire()) {
            for (int side = -1; side <= 1; side += 2) {
                GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(-0.38, 0.34, 0.56 * side), 0.3f,
                        new float[]{1f, 0.5f, 0.1f}, 0.8f);
            }
        }
        pose.popPose();
    }


    /** An eight-sided wheel: two square tyres turned 45 degrees to each other, around an axle along z. */
    private static void wheel(MultiBufferSource buffers, PoseStack pose, float x, float r, float z, float width,
                              float spin, int light) {
        for (int k = 0; k < 2; k++) {
            pose.pushPose();
            pose.translate(x, r, z);
            pose.mulPose(Axis.ZP.rotationDegrees(45 * k - spin));
            pose.mulPose(Axis.XP.rotationDegrees(90));
            float rr = k == 0 ? r : r * 0.98f;
            box(buffers, pose, -rr, -width / 2, -rr, rr, width / 2, rr, GarageSkin.TYRE, GarageSkin.TYRE_TOP, light);
            pose.popPose();
        }
    }
}
