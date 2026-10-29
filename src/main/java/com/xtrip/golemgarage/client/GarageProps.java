package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xtrip.golemgarage.block.entity.GarageControllerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The working parts of the transformed Garage: a wasteland car idling against the side wall, a hoist trolley
 * that rolls along the roof beam with its hook swinging, meshing gears and a twitching pressure gauge on the
 * wall, a spinning exhaust fan and a bench grinder.
 */
final class GarageProps {
    static final int CAR_BODY = 23, CAR_GLASS = 24, GEAR = 25, GAUGE = 26, CHROME = 27, GRILLE = 28;

    private GarageProps() {
    }

    static void render(GarageControllerBlockEntity garage, BlockRenderDispatcher blocks, float time, float s,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        BlockPos origin = garage.getBlockPos();
        BlockPos min = garage.getBayMin();
        BlockPos max = garage.getBayMax();
        if (min == null || max == null) {
            return;
        }
        trolley(garage, blocks, time, s, pose, buffers, light, origin, min, max);
        if (!garage.roomyEnough()) {
            return;
        }
        car(garage, blocks, time, s, pose, buffers, light, origin);
        wallMachinery(garage, time, s, pose, buffers, light, origin, min, max);
        grinder(garage, time, s, pose, buffers, light, origin);
    }

    private static VertexConsumer vc(MultiBufferSource buffers) {
        return buffers.getBuffer(RenderType.entityCutoutNoCull(GarageSkin.TEX));
    }

    private static void box(MultiBufferSource buffers, PoseStack pose, float x0, float y0, float z0, float x1, float y1,
                            float z1, int side, int top, int light) {
        GarageSkin.box(vc(buffers), pose.last(), x0, y0, z0, x1, y1, z1, side, top, light);
    }

    // =================================================================== the car
    private static void car(GarageControllerBlockEntity garage, BlockRenderDispatcher blocks, float time, float s,
                            PoseStack pose, MultiBufferSource buffers, int light, BlockPos origin) {
        Vec3 c = garage.carCenter();
        Direction f = garage.inward();
        pose.pushPose();
        pose.translate(c.x - origin.getX(), c.y - origin.getY(), c.z - origin.getZ());
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(-f.getStepZ(), f.getStepX())));
        pose.scale(s, s, s);

        // Wheels stay put while the body shakes on its springs.
        wheel(buffers, pose, 0.84f, 0.27f, 0.5f, 0.2f, light);
        wheel(buffers, pose, 0.84f, 0.27f, -0.5f, 0.2f, light);
        wheel(buffers, pose, -0.82f, 0.33f, 0.5f, 0.26f, light);
        wheel(buffers, pose, -0.82f, 0.33f, -0.5f, 0.26f, light);

        pose.pushPose();
        pose.translate(0, 0.01f * Mth.sin(time * 2.7f) + 0.005f * Mth.sin(time * 5.3f), 0);
        pose.mulPose(Axis.XP.rotationDegrees(0.5f * Mth.sin(time * 2.1f)));

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
        // Grille and a spiked ram on the front.
        box(buffers, pose, 1.12f, 0.43f, -0.4f, 1.17f, 0.7f, 0.4f, GRILLE, CHROME, light);
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
        // The supercharger sticking out of the bonnet, its belt pulley spinning.
        box(buffers, pose, 0.5f, 0.79f, -0.16f, 0.86f, 1.0f, 0.16f, CHROME, CHROME, light);
        box(buffers, pose, 0.56f, 1.0f, -0.12f, 0.8f, 1.13f, 0.12f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        for (int k = 0; k < 2; k++) {
            pose.pushPose();
            pose.translate(0.88f, 0.89f, 0);
            pose.mulPose(Axis.XP.rotationDegrees(time * 24f + k * 45f));
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
        float flick = 0.75f + 0.15f * Mth.sin(time * 1.7f) + 0.1f * Mth.sin(time * 4.9f);
        VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
        for (float z : new float[]{0.31f, -0.31f}) {
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(1.18, 0.61, z), 0.35f,
                    new float[]{1f, 0.9f, 0.6f}, 0.55f * flick);
        }
        // Backfire flashes from the side pipes.
        if ((long) time % 140 < 3) {
            for (int side = -1; side <= 1; side += 2) {
                GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(-0.38, 0.34, 0.56 * side), 0.3f,
                        new float[]{1f, 0.5f, 0.1f}, 0.8f);
            }
        }
        pose.popPose();
        pose.popPose();
    }

    /** An eight-sided wheel: two square tyres turned 45 degrees to each other, around an axle along z. */
    private static void wheel(MultiBufferSource buffers, PoseStack pose, float x, float r, float z, float width, int light) {
        for (int k = 0; k < 2; k++) {
            pose.pushPose();
            pose.translate(x, r, z);
            pose.mulPose(Axis.ZP.rotationDegrees(45 * k));
            pose.mulPose(Axis.XP.rotationDegrees(90));
            float rr = k == 0 ? r : r * 0.98f;
            box(buffers, pose, -rr, -width / 2, -rr, rr, width / 2, rr, GarageSkin.TYRE, GarageSkin.TYRE_TOP, light);
            pose.popPose();
        }
    }

    // =================================================================== hoist trolley
    private static void trolley(GarageControllerBlockEntity garage, BlockRenderDispatcher blocks, float time, float s,
                                PoseStack pose, MultiBufferSource buffers, int light, BlockPos origin, BlockPos min, BlockPos max) {
        Direction in = garage.inward();
        float cx = (min.getX() + max.getX() + 1) / 2f - origin.getX();
        float cz = (min.getZ() + max.getZ() + 1) / 2f - origin.getZ();
        float roof = max.getY() + 1 - origin.getY();
        float halfLen = (in.getAxis() == Direction.Axis.X ? max.getX() - min.getX() + 1 : max.getZ() - min.getZ() + 1) / 2f;
        // Idle, it rolls slowly back and forth; during a repair it parks over the lift.
        float travel = garage.isBusy() ? 0f : Mth.sin(time * 0.012f) * (halfLen - 0.7f);
        float speed = garage.isBusy() ? 0f : Mth.cos(time * 0.012f);
        pose.pushPose();
        pose.translate(cx + in.getStepX() * travel, roof - 0.28f, cz + in.getStepZ() * travel);
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(-in.getStepZ(), in.getStepX())));
        pose.scale(s, s, s);
        box(buffers, pose, -0.22f, -0.2f, -0.2f, 0.22f, -0.02f, 0.2f, GarageSkin.RUST, GarageSkin.SOOT, light);
        for (float x : new float[]{-0.14f, 0.14f}) {
            for (float z : new float[]{-0.21f, 0.17f}) {
                pose.pushPose();
                pose.translate(x, -0.02f, z + 0.02f);
                pose.mulPose(Axis.ZP.rotationDegrees(-time * 3f * speed));
                box(buffers, pose, -0.05f, -0.05f, -0.02f, 0.05f, 0.05f, 0.02f, GEAR, GEAR, light);
                pose.popPose();
            }
        }
        if (!garage.isBusy()) {
            // The hook swings, more when the trolley moves.
            pose.pushPose();
            pose.translate(0, -0.2f, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(4f * Mth.sin(time * 0.06f) + 6f * speed * Mth.sin(time * 0.02f)));
            pose.mulPose(Axis.XP.rotationDegrees(3f * Mth.cos(time * 0.05f)));
            float len = 0.9f;
            pose.pushPose();
            pose.translate(-0.5, -len, -0.5);
            pose.scale(1f, len, 1f);
            blocks.renderSingleBlock(Blocks.CHAIN.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            box(buffers, pose, -0.05f, -len - 0.08f, -0.05f, 0.05f, -len, 0.05f, CHROME, CHROME, light);
            box(buffers, pose, -0.03f, -len - 0.3f, -0.03f, 0.03f, -len - 0.08f, 0.03f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            box(buffers, pose, -0.03f, -len - 0.33f, -0.03f, 0.16f, -len - 0.27f, 0.03f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            box(buffers, pose, 0.1f, -len - 0.33f, -0.03f, 0.16f, -len - 0.2f, 0.03f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            pose.popPose();
        }
        pose.popPose();
    }

    // =================================================================== wall machinery
    private static void wallMachinery(GarageControllerBlockEntity garage, float time, float s, PoseStack pose,
                                      MultiBufferSource buffers, int light, BlockPos origin, BlockPos min, BlockPos max) {
        float floor = min.getY() - origin.getY();
        float height = max.getY() - min.getY() + 1;
        float cx = (min.getX() + max.getX() + 1) / 2f - origin.getX();
        float cz = (min.getZ() + max.getZ() + 1) / 2f - origin.getZ();

        // Gears and a gauge on the wall across from the car.
        Direction n = garage.carSide();
        Direction wall = n.getOpposite();
        float half = (wall.getAxis() == Direction.Axis.X ? max.getX() - min.getX() + 1 : max.getZ() - min.getZ() + 1) / 2f;
        pose.pushPose();
        pose.translate(cx + wall.getStepX() * half, floor, cz + wall.getStepZ() * half);
        pose.mulPose(Axis.YP.rotationDegrees(-n.toYRot()));
        // High on the wall, clear of any windows lower down.
        float gy = height - 0.85f;
        gear(buffers, pose, -0.38f, gy, 0.44f, 10, time * 1.6f, s, light);
        gear(buffers, pose, 0.36f, gy - 0.06f, 0.3f, 7, -time * 1.6f * 0.44f / 0.3f + 13f, s, light);
        gear(buffers, pose, -0.38f - 0.62f, gy + 0.1f, 0.18f, 5, -time * 1.6f * 0.44f / 0.18f + 20f, s, light);
        // Pressure gauge, its needle twitching.
        float gx = 1.05f, gyy = height - 0.7f;
        box(buffers, pose, gx - 0.17f, gyy - 0.17f, 0.01f, gx + 0.17f, gyy + 0.17f, 0.07f * s, GAUGE, CHROME, light);
        box(buffers, pose, gx - 0.03f, gyy + 0.17f, 0.02f, gx + 0.03f, height, 0.07f, CHROME, CHROME, light);
        pose.pushPose();
        pose.translate(gx, gyy, 0.075f);
        pose.mulPose(Axis.ZP.rotationDegrees(-30f + 50f * Mth.sin(time * 0.05f) + 8f * Mth.sin(time * 1.1f)));
        box(buffers, pose, -0.01f, -0.01f, 0, 0.13f * s, 0.01f, 0.01f, GarageSkin.JERRY, GarageSkin.JERRY, light);
        pose.popPose();
        pose.popPose();

        // An exhaust fan high on the controller wall.
        Direction in = garage.inward();
        float halfIn = (in.getAxis() == Direction.Axis.X ? max.getX() - min.getX() + 1 : max.getZ() - min.getZ() + 1) / 2f;
        pose.pushPose();
        pose.translate(cx - in.getStepX() * halfIn, floor, cz - in.getStepZ() * halfIn);
        pose.mulPose(Axis.YP.rotationDegrees(-in.toYRot()));
        float fx = 1.25f, fy = height - 0.7f;
        float r = 0.36f;
        box(buffers, pose, fx - r, fy - r, 0.01f, fx + r, fy - r + 0.06f, 0.12f, GarageSkin.RUST, GarageSkin.RUST, light);
        box(buffers, pose, fx - r, fy + r - 0.06f, 0.01f, fx + r, fy + r, 0.12f, GarageSkin.RUST, GarageSkin.RUST, light);
        box(buffers, pose, fx - r, fy - r, 0.01f, fx - r + 0.06f, fy + r, 0.12f, GarageSkin.RUST, GarageSkin.RUST, light);
        box(buffers, pose, fx + r - 0.06f, fy - r, 0.01f, fx + r, fy + r, 0.12f, GarageSkin.RUST, GarageSkin.RUST, light);
        box(buffers, pose, fx - r + 0.06f, fy - r + 0.06f, 0.01f, fx + r - 0.06f, fy + r - 0.06f, 0.015f, GRILLE, GRILLE, light);
        pose.pushPose();
        pose.translate(fx, fy, 0.07f);
        pose.mulPose(Axis.ZP.rotationDegrees(time * 22f));
        for (int b = 0; b < 4; b++) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(90 * b));
            pose.mulPose(Axis.XP.rotationDegrees(20));
            box(buffers, pose, 0.03f, -0.06f, -0.005f, (r - 0.08f) * s, 0.06f, 0.005f, CHROME, CHROME, light);
            pose.popPose();
        }
        box(buffers, pose, -0.05f, -0.05f, -0.02f, 0.05f, 0.05f, 0.03f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        pose.popPose();
        pose.popPose();
    }

    /** A spur gear on the wall plane (x along the wall, y up, z out of the wall), turned by angle degrees. */
    private static void gear(MultiBufferSource buffers, PoseStack pose, float x, float y, float r, int teeth, float angle,
                             float s, int light) {
        pose.pushPose();
        pose.translate(x, y, 0.05f);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.scale(s, s, 1f);
        float body = r * 0.78f;
        for (int k = 0; k < 2; k++) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(45 * k));
            box(buffers, pose, -body, -body, -0.03f, body, body, 0.03f, GEAR, GEAR, light);
            pose.popPose();
        }
        for (int t = 0; t < teeth; t++) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(360f * t / teeth));
            box(buffers, pose, body - 0.02f, -0.045f, -0.025f, r, 0.045f, 0.025f, GEAR, GEAR, light);
            pose.popPose();
        }
        box(buffers, pose, -0.06f, -0.06f, -0.04f, 0.06f, 0.06f, 0.05f, CHROME, CHROME, light);
        pose.popPose();
    }

    // =================================================================== bench grinder
    private static void grinder(GarageControllerBlockEntity garage, float time, float s, PoseStack pose,
                                MultiBufferSource buffers, int light, BlockPos origin) {
        Vec3 g = garage.grinderPos();
        if (g == null) {
            return;
        }
        float gx = (float) (g.x - origin.getX()), gy = (float) (g.y - origin.getY()), gz = (float) (g.z - origin.getZ());
        box(buffers, pose, gx - 0.2f, gy - 0.16f, gz - 0.07f, gx - 0.04f, gy + 0.0f, gz + 0.07f, CHROME, GarageSkin.SOOT, light);
        boolean on = GarageControllerBlockEntity.grinding(time);
        for (int k = 0; k < 2; k++) {
            pose.pushPose();
            pose.translate(gx, gy - 0.08f, gz);
            pose.mulPose(Axis.XP.rotationDegrees((on ? time * 50f : time * 2f) + 45 * k));
            pose.scale(s, s, s);
            box(buffers, pose, -0.03f, -0.1f, -0.1f, 0.03f, 0.1f, 0.1f, GarageSkin.SOOT, GEAR, light);
            pose.popPose();
        }
    }
}
