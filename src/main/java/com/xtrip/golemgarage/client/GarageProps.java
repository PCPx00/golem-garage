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
    static final int CAR_BODY = 23, CAR_GLASS = 24, GEAR = 25, GAUGE = 26, CHROME = 27, GRILLE = 28, HEADLIGHT = 38;

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
        switch (garage.style()) {
            case SHOWROOM -> toolWall(garage, s, pose, buffers, light, origin, min, max);
            case BARN -> barn(garage, blocks, time, pose, buffers, light, origin, min, max);
            case BUNKER -> searchlights(garage, time, pose, buffers, origin, min, max);
            default -> {
            }
        }
    }

    /** Showroom: the car sits on a two-post lift that slowly raises it and lets it down again. */
    private static float liftHeight(float time) {
        float t = (time % 1200f) / 1200f;
        float up = Mth.clamp((float) Math.sin(t * Math.PI * 2) * 1.6f, 0f, 1f);
        return up * 0.55f;
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
        float lift = 0f;
        if (garage.style() == com.xtrip.golemgarage.block.GarageStyle.SHOWROOM) {
            lift = liftHeight(time);
            // Two posts either side of the car, with arms reaching under the sills.
            for (int side = -1; side <= 1; side += 2) {
                float z = 0.78f * side;
                box(buffers, pose, -0.08f, 0f, z - 0.06f, 0.08f, 1.45f, z + 0.06f, GarageSkin.JERRY, GarageSkin.SOOT, light);
                box(buffers, pose, -0.2f, 0f, z - 0.14f, 0.2f, 0.05f, z + 0.14f, GarageSkin.SOOT, GarageSkin.SOOT, light);
                float zi = side > 0 ? 0.42f : -0.7f;
                box(buffers, pose, -0.5f, 0.2f + lift, zi, -0.42f, 0.25f + lift, zi + 0.28f, CHROME, CHROME, light);
                box(buffers, pose, 0.42f, 0.2f + lift, zi, 0.5f, 0.25f + lift, zi + 0.28f, CHROME, CHROME, light);
                box(buffers, pose, -0.5f, 0.2f + lift, side > 0 ? 0.7f : -0.7f, 0.5f, 0.27f + lift,
                        side > 0 ? 0.78f : -0.62f, CHROME, CHROME, light);
            }
            pose.translate(0, lift, 0);
        }

        CarModel.render(blocks, pose, buffers, light, time, new CarModel.Options(true, false, false, 0f, 0f, lift <= 0.01f,
                (long) time % 140 < 3));
        pose.popPose();
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

    // =================================================================== style props
    /**
     * A corner away from the car and the grinder: the nearest such corner, or the farthest. Null if every corner is
     * taken.
     */
    private static Vec3 freeCorner(GarageControllerBlockEntity garage, BlockPos min, BlockPos max, boolean far) {
        java.util.List<Vec3> corners = garage.corners();
        Direction car = garage.carSide();
        double cx = (min.getX() + max.getX() + 1) / 2.0, cz = (min.getZ() + max.getZ() + 1) / 2.0;
        Vec3 pick = null;
        for (int i = 1; i < corners.size(); i++) {
            Vec3 c = corners.get(i);
            if ((c.x - cx) * car.getStepX() + (c.z - cz) * car.getStepZ() < 0) {
                pick = c;
                if (!far) {
                    break;
                }
            }
        }
        return pick;
    }

    /** The corner a style prop stands in (Showroom tool cabinet, Barn crates), or null. */
    static Vec3 styleCorner(GarageControllerBlockEntity garage) {
        BlockPos min = garage.getBayMin(), max = garage.getBayMax();
        if (min == null || max == null || !garage.roomyEnough()) {
            return null;
        }
        return switch (garage.style()) {
            case SHOWROOM -> freeCorner(garage, min, max, false);
            case BARN -> freeCorner(garage, min, max, true);
            default -> null;
        };
    }

    /** Showroom: a red rolling tool cabinet in a free corner, with a pegboard of outlined tools above it. */
    private static void toolWall(GarageControllerBlockEntity garage, float s, PoseStack pose, MultiBufferSource buffers,
                                 int light, BlockPos origin, BlockPos min, BlockPos max) {
        Vec3 c = freeCorner(garage, min, max, false);
        if (c == null) {
            return;
        }
        Direction wall = garage.carSide().getOpposite();
        pose.pushPose();
        pose.translate(c.x - origin.getX(), c.y - origin.getY(), c.z - origin.getZ());
        // Face into the room, away from the wall behind.
        pose.mulPose(Axis.YP.rotationDegrees(-wall.getOpposite().toYRot()));
        // Local space: z toward the room, the wall at z = -0.5.
        box(buffers, pose, -0.36f, 0.06f, -0.46f, 0.36f, 0.95f, -0.02f, GarageSkin.JERRY, GarageSkin.SOOT, light);
        for (int k = 0; k < 5; k++) {
            float y = 0.14f + k * 0.16f;
            box(buffers, pose, -0.32f, y + 0.11f, -0.02f, 0.32f, y + 0.12f, 0.0f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            box(buffers, pose, -0.1f, y + 0.05f, -0.02f, 0.1f, y + 0.07f, 0.02f, CHROME, CHROME, light);
        }
        for (float x : new float[]{-0.3f, 0.3f}) {
            box(buffers, pose, x - 0.04f, 0f, -0.3f, x + 0.04f, 0.06f, -0.22f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        }
        // Pegboard on the wall above.
        float y0 = 1.05f, y1 = 1.85f;
        box(buffers, pose, -0.45f, y0, -0.5f, 0.45f, y1, -0.47f, GarageSkin.PLANKS, GarageSkin.PLANKS, light);
        for (int k = 0; k < 3; k++) {
            float x = -0.36f + k * 0.1f;
            float len = 0.25f + k * 0.07f;
            box(buffers, pose, x - 0.015f, y1 - 0.1f - len, -0.47f, x + 0.015f, y1 - 0.1f, -0.45f, CHROME, CHROME, light);
            box(buffers, pose, x - 0.035f, y1 - 0.14f, -0.47f, x + 0.035f, y1 - 0.08f, -0.45f, CHROME, CHROME, light);
        }
        box(buffers, pose, 0.0f, y1 - 0.55f, -0.47f, 0.03f, y1 - 0.12f, -0.45f, GarageSkin.BEAM_PLANKS, GarageSkin.BEAM_PLANKS, light);
        box(buffers, pose, -0.07f, y1 - 0.2f, -0.47f, 0.1f, y1 - 0.12f, -0.43f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        box(buffers, pose, 0.17f, y1 - 0.5f, -0.47f, 0.38f, y1 - 0.25f, -0.46f, CHROME, CHROME, light);
        box(buffers, pose, 0.34f, y1 - 0.27f, -0.47f, 0.4f, y1 - 0.12f, -0.44f, GarageSkin.BEAM_PLANKS, GarageSkin.BEAM_PLANKS, light);
        pose.popPose();
    }

    /** Barn: stacked crates in the far corner and lanterns swinging from the rafters. */
    private static void barn(GarageControllerBlockEntity garage, BlockRenderDispatcher blocks, float time, PoseStack pose,
                             MultiBufferSource buffers, int light, BlockPos origin, BlockPos min, BlockPos max) {
        Vec3 c = freeCorner(garage, min, max, true);
        if (c != null) {
            float x = (float) (c.x - origin.getX()), y = (float) (c.y - origin.getY()), z = (float) (c.z - origin.getZ());
            box(buffers, pose, x - 0.42f, y, z - 0.42f, x + 0.38f, y + 0.6f, z + 0.38f, GarageSkin.PLANKS, GarageSkin.BEAM_PLANKS, light);
            box(buffers, pose, x - 0.3f, y + 0.6f, z - 0.3f, x + 0.22f, y + 1.0f, z + 0.22f, GarageSkin.BEAM_PLANKS, GarageSkin.PLANKS, light);
            box(buffers, pose, x + 0.05f, y + 1.0f, z - 0.2f, x + 0.2f, y + 1.18f, z - 0.05f, GarageSkin.JERRY, GarageSkin.JERRY, light);
        }
        Direction in = garage.inward();
        float cx = (min.getX() + max.getX() + 1) / 2f - origin.getX();
        float cz = (min.getZ() + max.getZ() + 1) / 2f - origin.getZ();
        float roof = max.getY() + 1 - origin.getY();
        float halfIn = (in.getAxis() == Direction.Axis.X ? max.getX() - min.getX() + 1 : max.getZ() - min.getZ() + 1) / 2f;
        for (int k = -1; k <= 1; k += 2) {
            float off = k * halfIn * 0.5f;
            pose.pushPose();
            pose.translate(cx + in.getStepX() * off, roof, cz + in.getStepZ() * off);
            pose.mulPose(Axis.ZP.rotationDegrees(5f * Mth.sin(time * 0.05f + k)));
            pose.mulPose(Axis.XP.rotationDegrees(4f * Mth.cos(time * 0.04f + k)));
            pose.translate(-0.5, -1.0, -0.5);
            blocks.renderSingleBlock(Blocks.LANTERN.defaultBlockState().setValue(
                    net.minecraft.world.level.block.LanternBlock.HANGING, true), pose, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    /** Bunker: two searchlights on the roof, sweeping the yard. */
    private static void searchlights(GarageControllerBlockEntity garage, float time, PoseStack pose,
                                     MultiBufferSource buffers, BlockPos origin, BlockPos min, BlockPos max) {
        float top = max.getY() + 2 - origin.getY();
        float[][] spots = {{min.getX() + 0.5f, min.getZ() + 0.5f}, {max.getX() + 0.5f, max.getZ() + 0.5f}};
        int light = 0xF000F0;
        for (int i = 0; i < 2; i++) {
            float x = spots[i][0] - origin.getX(), z = spots[i][1] - origin.getZ();
            pose.pushPose();
            pose.translate(x, top, z);
            box(buffers, pose, -0.2f, 0, -0.2f, 0.2f, 0.08f, 0.2f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            box(buffers, pose, -0.04f, 0.08f, -0.04f, 0.04f, 0.32f, 0.04f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            pose.mulPose(Axis.YP.rotationDegrees(time * 1.2f + i * 180f));
            pose.translate(0, 0.4f, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(18f));
            box(buffers, pose, -0.18f, -0.14f, -0.14f, 0.16f, 0.14f, 0.14f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            box(buffers, pose, 0.16f, -0.12f, -0.12f, 0.19f, 0.12f, 0.12f, HEADLIGHT, CHROME, light);
            VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(0.24, 0, 0), 0.32f,
                    new float[]{1f, 0.97f, 0.85f}, 0.9f);
            for (int k = 1; k <= 7; k++) {
                float d = k * 0.9f;
                GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(0.2 + d, 0, 0), 0.18f + k * 0.11f,
                        new float[]{1f, 0.97f, 0.85f}, 0.32f - k * 0.035f);
            }
            pose.popPose();
        }
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
