package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xtrip.golemgarage.entity.RigVariant;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The wasteland rigs, drawn from boxes of the Garage skin. Shared by the car idling in a finished Garage, the
 * drivable rigs and their items. Car space: x forward, y up from the ground, z across; about 2.5 long and 1.2 wide.
 */
public final class CarModel {
    static final int CAR_BODY = 23, CAR_GLASS = 24, GEAR = 25, CHROME = 27, GRILLE = 28;
    static final int RED = 29, STRIPE = 30, OLIVE = 31, TRACK = 32, TRACK_TOP = 33, TEAL = 34, SKIRT = 35, BLADE = 36,
            NITRO = 37, HEADLIGHT = 38, HUB = 39, TAILLIGHT = 40, MESH = 41, ARMOUR = 42;

    /**
     * What to draw: which rig, the upgrades fitted, how far the wheels have turned, where the turret points, whether
     * the engine runs, a backfire or nitro flame, and a cannon flash (0 to 1).
     */
    public record Options(RigVariant variant, boolean ram, boolean turret, boolean plating, float wheelAngle,
                          float turretYaw, boolean engine, boolean backfire, boolean boost, float flash) {
        public Options(boolean ram, boolean turret, boolean plating, float wheelAngle, float turretYaw, boolean engine,
                       boolean backfire) {
            this(RigVariant.WAR_RIG, ram, turret, plating, wheelAngle, turretYaw, engine, backfire, false, 0f);
        }
    }

    private CarModel() {
    }

    /** A spray-can paint job: the tile that replaces the body paint, or -1. Set around one render call. */
    private static int paint = -1;
    /** Rig gadgets fitted (RigEntity upgrade bits) and whether the headlights are on. Set around one render call. */
    private static int gadgets = 0;
    private static boolean lights = true;

    /** The skin tile for a spray-can finish id ("flames", ...), or -1. */
    public static int finishTile(String finish) {
        return switch (finish) {
            case "flames" -> 62;
            case "hazard" -> 63;
            case "rust" -> 64;
            case "camo" -> 65;
            case "chrome" -> 66;
            case "tiger" -> 67;
            default -> -1;
        };
    }

    private static int painted(int tile) {
        if (paint < 0) {
            return tile;
        }
        return tile == CAR_BODY || tile == RED || tile == STRIPE || tile == OLIVE || tile == ARMOUR || tile == TEAL ? paint : tile;
    }

    private static void box(MultiBufferSource buffers, PoseStack pose, float x0, float y0, float z0, float x1, float y1,
                            float z1, int side, int top, int light) {
        side = painted(side);
        top = painted(top);
        GarageSkin.box(buffers.getBuffer(RenderType.entityCutoutNoCull(GarageSkin.TEX)), pose.last(),
                x0, y0, z0, x1, y1, z1, side, top, light);
    }

    /** The same box on both sides of the car (z0..z1 given for the +z side). */
    private static void pair(MultiBufferSource buffers, PoseStack pose, float x0, float y0, float z0, float x1, float y1,
                             float z1, int side, int top, int light) {
        box(buffers, pose, x0, y0, z0, x1, y1, z1, side, top, light);
        box(buffers, pose, x0, y0, -z1, x1, y1, -z0, side, top, light);
    }

    public static void render(BlockRenderDispatcher blocks, PoseStack pose, MultiBufferSource buffers, int light,
                              float time, Options o) {
        render(blocks, pose, buffers, light, time, o, -1);
    }

    public static void render(BlockRenderDispatcher blocks, PoseStack pose, MultiBufferSource buffers, int light,
                              float time, Options o, int paintTile) {
        render(blocks, pose, buffers, light, time, o, paintTile, 0, true);
    }

    public static void render(BlockRenderDispatcher blocks, PoseStack pose, MultiBufferSource buffers, int light,
                              float time, Options o, int paintTile, int gadgetBits, boolean lightsOn) {
        paint = paintTile;
        gadgets = gadgetBits;
        lights = lightsOn;
        try {
            renderModel(blocks, pose, buffers, light, time, o);
        } finally {
            paint = -1;
            gadgets = 0;
            lights = true;
        }
    }

    private static void renderModel(BlockRenderDispatcher blocks, PoseStack pose, MultiBufferSource buffers, int light,
                                    float time, Options o) {
        switch (o.variant()) {
            case INTERCEPTOR -> interceptor(blocks, pose, buffers, light, time, o);
            case JUGGERNAUT -> juggernaut(blocks, pose, buffers, light, time, o);
            case SKIMMER -> skimmer(blocks, pose, buffers, light, time, o);
            default -> warRig(blocks, pose, buffers, light, time, o);
        }
    }

    /** Engine shake: the body bounces on its springs while it idles. */
    private static float shake(PoseStack pose, float time, Options o, float amount) {
        float s = o.engine() ? amount : 0f;
        pose.translate(0, s * (0.01f * Mth.sin(time * 2.7f) + 0.005f * Mth.sin(time * 5.3f)), 0);
        pose.mulPose(Axis.XP.rotationDegrees(s * 0.5f * Mth.sin(time * 2.1f)));
        return s;
    }

    // =================================================================== War Rig
    private static void warRig(BlockRenderDispatcher blocks, PoseStack pose, MultiBufferSource buffers, int light,
                               float time, Options o) {
        wheel(buffers, pose, 0.84f, 0.27f, 0.5f, 0.2f, o.wheelAngle(), light);
        wheel(buffers, pose, 0.84f, 0.27f, -0.5f, 0.2f, o.wheelAngle(), light);
        wheel(buffers, pose, -0.82f, 0.33f, 0.52f, 0.26f, o.wheelAngle() * 0.82f, light);
        wheel(buffers, pose, -0.82f, 0.33f, -0.52f, 0.26f, o.wheelAngle() * 0.82f, light);

        pose.pushPose();
        float shake = shake(pose, time, o, 1f);
        box(buffers, pose, -1.25f, 0.24f, -0.46f, 1.25f, 0.42f, 0.46f, GarageSkin.RUST, GarageSkin.RUST, light);
        box(buffers, pose, -1.18f, 0.4f, -0.5f, 1.12f, 0.72f, 0.5f, CAR_BODY, CAR_BODY, light);
        box(buffers, pose, 0.24f, 0.72f, -0.46f, 1.12f, 0.79f, 0.46f, CAR_BODY, CAR_BODY, light);
        // Open-top cab: doors, a mesh guard where the windscreen was, and the A-pillars.
        pair(buffers, pose, -0.72f, 0.72f, 0.4f, 0.22f, 0.84f, 0.46f, CAR_BODY, CAR_BODY, light);
        box(buffers, pose, 0.2f, 0.72f, -0.43f, 0.24f, 1.08f, 0.43f, MESH, MESH, light);
        pair(buffers, pose, 0.18f, 0.72f, 0.41f, 0.26f, 1.3f, 0.47f, CAR_BODY, CAR_BODY, light);
        pair(buffers, pose, -0.76f, 0.72f, 0.41f, -0.68f, 1.3f, 0.47f, CAR_BODY, CAR_BODY, light);
        box(buffers, pose, -1.18f, 0.72f, -0.47f, -0.72f, 0.8f, 0.47f, GarageSkin.RUST, GarageSkin.RUST, light);
        // Wheel arches: flared rust fenders over each wheel.
        pair(buffers, pose, 0.56f, 0.56f, 0.5f, 1.12f, 0.62f, 0.62f, GarageSkin.RUST, GarageSkin.RUST, light);
        pair(buffers, pose, -1.16f, 0.66f, 0.5f, -0.48f, 0.72f, 0.66f, GarageSkin.RUST, GarageSkin.RUST, light);
        // Roll cage over the cabin, and a light bar.
        for (float z : new float[]{-0.47f, 0.43f}) {
            box(buffers, pose, -0.76f, 1.3f, z, 0.26f, 1.36f, z + 0.04f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        }
        box(buffers, pose, 0.2f, 1.3f, -0.47f, 0.26f, 1.36f, 0.47f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        box(buffers, pose, -0.76f, 1.3f, -0.47f, -0.7f, 1.36f, 0.47f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        for (float z : new float[]{-0.3f, -0.1f, 0.1f, 0.3f}) {
            box(buffers, pose, 0.22f, 1.36f, z - 0.06f, 0.3f, 1.44f, z + 0.06f, HEADLIGHT, CHROME, light);
        }
        // Wing mirrors.
        pair(buffers, pose, 0.12f, 0.82f, 0.46f, 0.18f, 0.9f, 0.58f, CHROME, CHROME, light);
        // Grille, and a spiked ram on the front (or a bull bar).
        box(buffers, pose, 1.12f, 0.43f, -0.4f, 1.17f, 0.7f, 0.4f, GRILLE, CHROME, light);
        if (o.ram()) {
            ram(blocks, buffers, pose, 1.17f, light);
        } else {
            box(buffers, pose, 1.15f, 0.32f, -0.48f, 1.22f, 0.42f, 0.48f, CHROME, CHROME, light);
            pair(buffers, pose, 1.17f, 0.42f, 0.3f, 1.22f, 0.72f, 0.35f, CHROME, CHROME, light);
            box(buffers, pose, 1.17f, 0.7f, -0.35f, 1.22f, 0.74f, 0.35f, CHROME, CHROME, light);
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
        // Side pipes and twin stacks behind the cabin.
        for (int side = -1; side <= 1; side += 2) {
            float z0 = side > 0 ? 0.5f : -0.6f;
            box(buffers, pose, -0.34f, 0.3f, z0, 0.7f, 0.38f, z0 + 0.1f, CHROME, CHROME, light);
            float sz = 0.36f * side;
            box(buffers, pose, -0.68f, 0.72f, sz - 0.05f, -0.56f, 1.48f, sz + 0.05f, CHROME, GarageSkin.SOOT, light);
        }
        // Spare tyre and a jerry can on the back deck.
        box(buffers, pose, -1.12f, 0.8f, -0.3f, -0.78f, 0.9f, 0.04f, GarageSkin.TYRE, GarageSkin.TYRE_TOP, light);
        box(buffers, pose, -1.1f, 0.8f, 0.12f, -0.86f, 1.08f, 0.26f, GarageSkin.JERRY, GarageSkin.JERRY, light);
        // Head and tail lights.
        pair(buffers, pose, 1.11f, 0.56f, 0.24f, 1.16f, 0.66f, 0.38f, HEADLIGHT, CHROME, light);
        pair(buffers, pose, -1.22f, 0.56f, 0.32f, -1.17f, 0.66f, 0.46f, TAILLIGHT, TAILLIGHT, light);
        if (o.plating()) {
            for (int side = -1; side <= 1; side += 2) {
                float z0 = side > 0 ? 0.5f : -0.54f;
                box(buffers, pose, -0.9f, 0.42f, z0, 0.9f, 0.74f, z0 + 0.04f, GarageSkin.PLATES, GarageSkin.RUST, light);
                box(buffers, pose, -0.7f, 0.76f, z0 * 0.92f, 0.2f, 1.06f, z0 * 0.92f + 0.03f, GRILLE, GRILLE, light);
            }
            box(buffers, pose, 0.22f, 0.76f, -0.42f, 0.25f, 1.08f, 0.42f, GRILLE, GRILLE, light);
        }
        if (o.turret()) {
            turret(buffers, pose, -0.3f, 1.36f, o.turretYaw(), light);
        }
        float flick = flicker(time, o);
        VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
        for (float z : new float[]{0.31f, -0.31f}) {
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(1.18, 0.61, z), 0.35f,
                    new float[]{1f, 0.9f, 0.6f}, 0.55f * flick);
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(-1.24, 0.61, z * 1.25), 0.2f,
                    new float[]{1f, 0.2f, 0.1f}, 0.5f * flick);
        }
        if (o.backfire()) {
            for (int side = -1; side <= 1; side += 2) {
                GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(-0.38, 0.34, 0.56 * side), 0.3f,
                        new float[]{1f, 0.5f, 0.1f}, 0.8f);
            }
        }
        gadgets(blocks, pose, buffers, light, time, o);
        pose.popPose();
    }

    // =================================================================== Interceptor
    private static void interceptor(BlockRenderDispatcher blocks, PoseStack pose, MultiBufferSource buffers, int light,
                                    float time, Options o) {
        wheel(buffers, pose, 0.86f, 0.22f, 0.5f, 0.18f, o.wheelAngle() * 1.2f, light);
        wheel(buffers, pose, 0.86f, 0.22f, -0.5f, 0.18f, o.wheelAngle() * 1.2f, light);
        wheel(buffers, pose, -0.8f, 0.31f, 0.53f, 0.32f, o.wheelAngle() * 0.85f, light);
        wheel(buffers, pose, -0.8f, 0.31f, -0.53f, 0.32f, o.wheelAngle() * 0.85f, light);

        pose.pushPose();
        float shake = shake(pose, time, o, 0.6f);
        // Low chassis and a long red body with stripes over the bonnet, roof and boot.
        box(buffers, pose, -1.3f, 0.18f, -0.44f, 1.3f, 0.3f, 0.44f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        box(buffers, pose, -1.24f, 0.28f, -0.48f, 1.2f, 0.56f, 0.48f, RED, STRIPE, light);
        box(buffers, pose, 0.3f, 0.56f, -0.44f, 1.2f, 0.62f, 0.44f, RED, STRIPE, light);
        box(buffers, pose, -1.24f, 0.56f, -0.46f, -0.6f, 0.64f, 0.46f, RED, STRIPE, light);
        // Fat rear arches and slim front ones.
        pair(buffers, pose, -1.14f, 0.5f, 0.46f, -0.46f, 0.64f, 0.7f, RED, RED, light);
        pair(buffers, pose, 0.62f, 0.44f, 0.46f, 1.1f, 0.5f, 0.6f, RED, RED, light);
        // A low, raked cabin.
        box(buffers, pose, -0.6f, 0.56f, -0.4f, 0.3f, 0.86f, 0.4f, CAR_GLASS, STRIPE, light);
        box(buffers, pose, 0.3f, 0.62f, -0.38f, 0.42f, 0.78f, 0.38f, CAR_GLASS, CAR_GLASS, light);
        // Front splitter, nose and headlights.
        box(buffers, pose, 1.18f, 0.18f, -0.5f, 1.36f, 0.24f, 0.5f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        box(buffers, pose, 1.2f, 0.3f, -0.36f, 1.26f, 0.5f, 0.36f, GRILLE, CHROME, light);
        pair(buffers, pose, 1.19f, 0.42f, 0.36f, 1.24f, 0.52f, 0.46f, HEADLIGHT, CHROME, light);
        pair(buffers, pose, -1.28f, 0.42f, 0.28f, -1.23f, 0.52f, 0.44f, TAILLIGHT, TAILLIGHT, light);
        if (o.ram()) {
            ram(blocks, buffers, pose, 1.26f, light);
        }
        // The blower punching through the bonnet, with its scoop.
        box(buffers, pose, 0.52f, 0.62f, -0.15f, 0.9f, 0.8f, 0.15f, CHROME, CHROME, light);
        box(buffers, pose, 0.56f, 0.8f, -0.12f, 0.86f, 0.9f, 0.12f, GarageSkin.SOOT, GRILLE, light);
        for (int k = 0; k < 2; k++) {
            pose.pushPose();
            pose.translate(0.89f, 0.72f, 0);
            pose.mulPose(Axis.XP.rotationDegrees(shake * time * 40f + k * 45f));
            box(buffers, pose, -0.02f, -0.07f, -0.07f, 0.02f, 0.07f, 0.07f, GEAR, CHROME, light);
            pose.popPose();
        }
        // Zoomie pipes: four short chrome stubs either side, angled back.
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < 4; k++) {
                float x = 0.28f + k * 0.14f;
                float z = 0.48f * side;
                box(buffers, pose, x - 0.03f, 0.36f, side > 0 ? z : z - 0.06f, x + 0.03f, 0.44f,
                        side > 0 ? z + 0.06f : z, CHROME, GarageSkin.SOOT, light);
            }
        }
        // Rear wing on two struts.
        pair(buffers, pose, -1.14f, 0.64f, 0.3f, -1.08f, 0.94f, 0.34f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        box(buffers, pose, -1.3f, 0.94f, -0.56f, -0.98f, 0.99f, 0.56f, RED, STRIPE, light);
        pair(buffers, pose, -1.3f, 0.86f, 0.56f, -0.98f, 1.02f, 0.59f, RED, RED, light);
        // Twin nitro bottles behind the seats.
        pair(buffers, pose, -0.98f, 0.64f, 0.08f, -0.62f, 0.78f, 0.24f, NITRO, CHROME, light);
        if (o.turret()) {
            turret(buffers, pose, -0.85f, 0.64f, o.turretYaw(), light);
        }
        if (o.plating()) {
            pair(buffers, pose, -0.55f, 0.3f, 0.48f, 0.55f, 0.54f, 0.52f, GarageSkin.PLATES, GarageSkin.RUST, light);
        }
        float flick = flicker(time, o);
        VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
        for (float z : new float[]{0.41f, -0.41f}) {
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(1.26, 0.47, z), 0.3f,
                    new float[]{1f, 0.95f, 0.75f}, 0.6f * flick);
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(-1.3, 0.47, z * 0.9), 0.18f,
                    new float[]{1f, 0.15f, 0.1f}, 0.55f * flick);
        }
        if (o.boost() || o.backfire()) {
            float[] col = o.boost() ? new float[]{0.4f, 0.6f, 1f} : new float[]{1f, 0.5f, 0.1f};
            float pulse = 0.7f + 0.3f * Mth.sin(time * 3f);
            for (int side = -1; side <= 1; side += 2) {
                for (int k = 0; k < 4; k++) {
                    GarageControllerRenderer.softGlow(glow, pose.last().pose(),
                            new Vec3(0.28 + k * 0.14, 0.4, 0.58 * side), 0.16f, col, 0.8f * pulse);
                }
            }
            if (o.boost()) {
                GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(-1.45, 0.3, 0), 0.7f,
                        new float[]{0.45f, 0.7f, 1f}, 0.7f * pulse);
            }
        }
        gadgets(blocks, pose, buffers, light, time, o);
        pose.popPose();
    }

    // =================================================================== Juggernaut
    private static void juggernaut(BlockRenderDispatcher blocks, PoseStack pose, MultiBufferSource buffers, int light,
                                   float time, Options o) {
        pose.pushPose();
        shake(pose, time, o, 0.5f);
        // Tracks: long belts with sloped ends, road wheels turning along the outside.
        for (int side = -1; side <= 1; side += 2) {
            float z0 = side > 0 ? 0.47f : -0.79f, z1 = z0 + 0.32f;
            box(buffers, pose, -1.2f, 0.02f, z0, 1.2f, 0.44f, z1, TRACK, TRACK_TOP, light);
            box(buffers, pose, 1.2f, 0.1f, z0, 1.34f, 0.38f, z1, TRACK, TRACK_TOP, light);
            box(buffers, pose, -1.34f, 0.1f, z0, -1.2f, 0.38f, z1, TRACK, TRACK_TOP, light);
            float zo = side > 0 ? z1 : z0;
            for (int k = 0; k < 5; k++) {
                float x = -0.96f + k * 0.48f;
                hub(buffers, pose, x, 0.21f, zo, 0.15f, side, o.wheelAngle() * 1.6f, HUB, light);
            }
        }
        // Hull, sloped glacis and side skirts.
        box(buffers, pose, -1.2f, 0.36f, -0.48f, 1.16f, 0.8f, 0.48f, ARMOUR, OLIVE, light);
        box(buffers, pose, 1.16f, 0.36f, -0.46f, 1.3f, 0.66f, 0.46f, ARMOUR, OLIVE, light);
        box(buffers, pose, 1.3f, 0.4f, -0.4f, 1.38f, 0.56f, 0.4f, ARMOUR, OLIVE, light);
        pair(buffers, pose, -1.24f, 0.44f, 0.47f, 1.26f, 0.56f, 0.82f, ARMOUR, OLIVE, light);
        // Driver's hatch and vision slit.
        box(buffers, pose, 0.6f, 0.8f, -0.36f, 1.0f, 0.86f, -0.04f, ARMOUR, OLIVE, light);
        box(buffers, pose, 1.0f, 0.82f, -0.34f, 1.02f, 0.85f, -0.06f, CAR_GLASS, CAR_GLASS, light);
        // Headlights in cages, exhaust stacks and jerry cans on the back.
        pair(buffers, pose, 1.3f, 0.6f, 0.3f, 1.37f, 0.7f, 0.42f, HEADLIGHT, GarageSkin.SOOT, light);
        pair(buffers, pose, -1.28f, 0.6f, 0.3f, -1.2f, 0.7f, 0.42f, TAILLIGHT, GarageSkin.SOOT, light);
        pair(buffers, pose, -1.1f, 0.8f, 0.22f, -0.98f, 1.34f, 0.34f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        box(buffers, pose, -1.3f, 0.5f, -0.2f, -1.2f, 0.78f, 0.02f, GarageSkin.JERRY, GarageSkin.JERRY, light);
        box(buffers, pose, -1.3f, 0.5f, 0.06f, -1.2f, 0.78f, 0.24f, GarageSkin.JERRY, GarageSkin.JERRY, light);
        if (o.ram()) {
            ram(blocks, buffers, pose, 1.38f, light);
        }
        if (o.plating()) {
            pair(buffers, pose, -0.9f, 0.56f, 0.82f, 0.9f, 0.6f, 0.86f, GarageSkin.PLATES, GarageSkin.RUST, light);
            box(buffers, pose, 1.38f, 0.38f, -0.44f, 1.42f, 0.6f, 0.44f, GarageSkin.PLATES, GarageSkin.RUST, light);
        }
        // The turret: turns to its target, the barrel kicks back when it fires.
        pose.pushPose();
        pose.translate(-0.15f, 0.8f, 0);
        box(buffers, pose, -0.46f, 0, -0.42f, 0.46f, 0.05f, 0.42f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        pose.mulPose(Axis.YP.rotationDegrees(o.turretYaw()));
        box(buffers, pose, -0.44f, 0.05f, -0.36f, 0.34f, 0.38f, 0.36f, ARMOUR, OLIVE, light);
        box(buffers, pose, 0.34f, 0.08f, -0.3f, 0.44f, 0.34f, 0.3f, ARMOUR, OLIVE, light);
        box(buffers, pose, 0.44f, 0.1f, -0.16f, 0.54f, 0.3f, 0.16f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        float kick = 0.14f * o.flash();
        box(buffers, pose, 0.54f - kick, 0.16f, -0.05f, 1.62f - kick, 0.25f, 0.05f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        box(buffers, pose, 1.56f - kick, 0.14f, -0.07f, 1.7f - kick, 0.27f, 0.07f, GarageSkin.SOOT, GRILLE, light);
        box(buffers, pose, -0.3f, 0.38f, 0.04f, 0.02f, 0.44f, 0.3f, ARMOUR, OLIVE, light);
        box(buffers, pose, -0.38f, 0.38f, -0.3f, -0.35f, 0.92f, -0.27f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        if (o.flash() > 0) {
            VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(1.85, 0.2, 0), 0.6f * o.flash() + 0.2f,
                    new float[]{1f, 0.75f, 0.3f}, o.flash());
        }
        pose.popPose();
        float flick = flicker(time, o);
        VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
        for (float z : new float[]{0.36f, -0.36f}) {
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(1.38, 0.65, z), 0.3f,
                    new float[]{1f, 0.9f, 0.6f}, 0.5f * flick);
        }
        gadgets(blocks, pose, buffers, light, time, o);
        pose.popPose();
    }

    // =================================================================== Swamp Skimmer
    private static void skimmer(BlockRenderDispatcher blocks, PoseStack pose, MultiBufferSource buffers, int light,
                                float time, Options o) {
        pose.pushPose();
        // A hovercraft bobs on its cushion of air.
        float hover = o.engine() ? 0.05f + 0.02f * Mth.sin(time * 0.35f) : 0f;
        pose.translate(0, hover, 0);
        shake(pose, time, o, 0.4f);
        // Rubber skirt all round, then the hull with a plank deck.
        box(buffers, pose, -1.2f, 0.0f, -0.62f, 1.14f, 0.28f, 0.62f, SKIRT, SKIRT, light);
        box(buffers, pose, 1.14f, 0.04f, -0.52f, 1.3f, 0.28f, 0.52f, SKIRT, SKIRT, light);
        box(buffers, pose, -1.28f, 0.04f, -0.52f, -1.2f, 0.28f, 0.52f, SKIRT, SKIRT, light);
        box(buffers, pose, -1.16f, 0.28f, -0.56f, 1.12f, 0.56f, 0.56f, TEAL, GarageSkin.PLANKS, light);
        box(buffers, pose, 0.7f, 0.56f, -0.5f, 1.22f, 0.64f, 0.5f, TEAL, TEAL, light);
        box(buffers, pose, 1.12f, 0.36f, -0.5f, 1.24f, 0.56f, 0.5f, TEAL, TEAL, light);
        // Gunwales.
        pair(buffers, pose, -1.16f, 0.56f, 0.5f, 0.7f, 0.64f, 0.56f, TEAL, TEAL, light);
        // Windscreen and dash.
        box(buffers, pose, 0.48f, 0.56f, -0.42f, 0.54f, 0.7f, 0.42f, CAR_GLASS, CHROME, light);
        box(buffers, pose, 0.3f, 0.56f, -0.3f, 0.46f, 0.7f, 0.3f, GarageSkin.SOOT, CHROME, light);
        pair(buffers, pose, 1.2f, 0.42f, 0.32f, 1.25f, 0.52f, 0.44f, HEADLIGHT, CHROME, light);
        // Life ring on the side.
        pair(buffers, pose, -0.3f, 0.32f, 0.56f, 0.02f, 0.52f, 0.6f, TAILLIGHT, TAILLIGHT, light);
        if (o.ram()) {
            ram(blocks, buffers, pose, 1.28f, light);
        }
        if (o.plating()) {
            pair(buffers, pose, -0.8f, 0.6f, 0.56f, 0.4f, 0.8f, 0.6f, GarageSkin.PLATES, GarageSkin.RUST, light);
        }
        // Engine block feeding the fan.
        box(buffers, pose, -0.95f, 0.56f, -0.22f, -0.6f, 0.92f, 0.22f, GarageSkin.SOOT, CHROME, light);
        box(buffers, pose, -0.85f, 0.92f, -0.08f, -0.7f, 1.0f, 0.08f, CHROME, CHROME, light);
        pair(buffers, pose, -0.8f, 0.7f, 0.22f, -0.7f, 1.2f, 0.28f, CHROME, GarageSkin.SOOT, light);
        // The fan in its wire cage, on struts.
        pair(buffers, pose, -1.06f, 0.56f, 0.48f, -1.0f, 1.66f, 0.54f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        box(buffers, pose, -1.06f, 1.6f, -0.54f, -1.0f, 1.66f, 0.54f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        float spin = o.engine() ? time * 55f : time * 2f;
        pose.pushPose();
        pose.translate(-1.04f, 1.1f, 0);
        box(buffers, pose, -0.06f, -0.07f, -0.07f, 0.06f, 0.07f, 0.07f, CHROME, CHROME, light);
        for (int k = 0; k < 3; k++) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(spin + k * 120));
            box(buffers, pose, -0.015f, 0.04f, -0.07f, 0.015f, 0.5f, 0.07f, BLADE, BLADE, light);
            pose.popPose();
        }
        pose.popPose();
        box(buffers, pose, -1.18f, 0.56f, -0.56f, -1.1f, 1.66f, 0.56f, MESH, MESH, light);
        box(buffers, pose, -0.98f, 0.56f, -0.56f, -0.9f, 1.66f, 0.56f, MESH, MESH, light);
        // Rudders behind the fan.
        for (float z : new float[]{-0.3f, 0f, 0.3f}) {
            box(buffers, pose, -1.36f, 0.62f, z - 0.015f, -1.2f, 1.5f, z + 0.015f, TEAL, TEAL, light);
        }
        if (o.turret()) {
            turret(buffers, pose, 0.0f, 0.56f, o.turretYaw(), light);
        }
        float flick = flicker(time, o);
        VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
        for (float z : new float[]{0.38f, -0.38f}) {
            GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(1.26, 0.47, z), 0.3f,
                    new float[]{1f, 0.95f, 0.75f}, 0.55f * flick);
        }
        gadgets(blocks, pose, buffers, light, time, o);
        pose.popPose();
    }


    // =================================================================== gadgets (0.27)
    private static final int G_HARPOON = 8, G_SMOKE = 16, G_SPIKES = 32, G_RACK = 64;

    /**
     * The bolt-on gadgets and the headlight beams, placed per rig: the front bumper, the bonnet, the back and the
     * roof (or deck) where a rack fits.
     */
    private static void gadgets(BlockRenderDispatcher blocks, PoseStack pose, MultiBufferSource buffers, int light,
                                float time, Options o) {
        float front, hood, rear, rackX0, rackX1, rackY, lampY, lampZ;
        switch (o.variant()) {
            case INTERCEPTOR -> {
                front = 1.3f; hood = 0.62f; rear = -1.3f; rackX0 = -0.55f; rackX1 = 0.25f; rackY = 0.86f; lampY = 0.47f; lampZ = 0.41f;
            }
            case JUGGERNAUT -> {
                front = 1.38f; hood = 0.8f; rear = -1.32f; rackX0 = -1.2f; rackX1 = -0.66f; rackY = 0.8f; lampY = 0.65f; lampZ = 0.36f;
            }
            case SKIMMER -> {
                front = 1.28f; hood = 0.64f; rear = -1.36f; rackX0 = 0.72f; rackX1 = 1.2f; rackY = 0.64f; lampY = 0.47f; lampZ = 0.38f;
            }
            default -> {
                front = 1.22f; hood = 0.79f; rear = -1.25f; rackX0 = -0.74f; rackX1 = 0.24f; rackY = 1.36f; lampY = 0.61f; lampZ = 0.31f;
            }
        }
        if ((gadgets & G_HARPOON) != 0) {
            // A harpoon launcher on the bonnet's right side, spear loaded.
            float x = front - 0.5f, z = -0.26f;
            box(buffers, pose, x - 0.12f, hood, z - 0.09f, x + 0.12f, hood + 0.08f, z + 0.09f, GarageSkin.SOOT, GarageSkin.RUST, light);
            box(buffers, pose, x - 0.2f, hood + 0.08f, z - 0.05f, x + 0.3f, hood + 0.17f, z + 0.05f, GarageSkin.RUST, GarageSkin.SOOT, light);
            box(buffers, pose, x + 0.3f, hood + 0.1f, z - 0.015f, x + 0.62f, hood + 0.14f, z + 0.015f, CHROME, CHROME, light);
            box(buffers, pose, x + 0.62f, hood + 0.08f, z - 0.04f, x + 0.72f, hood + 0.16f, z + 0.04f, CHROME, GarageSkin.SOOT, light);
            box(buffers, pose, x - 0.24f, hood + 0.07f, z - 0.08f, x - 0.18f, hood + 0.19f, z + 0.08f, GEAR, CHROME, light);
        }
        if ((gadgets & G_SMOKE) != 0) {
            // Two smoke canisters strapped to the back.
            for (float z : new float[]{-0.36f, -0.2f}) {
                box(buffers, pose, rear - 0.12f, 0.36f, z - 0.065f, rear, 0.68f, z + 0.065f, GarageSkin.SOOT, CHROME, light);
                box(buffers, pose, rear - 0.1f, 0.68f, z - 0.03f, rear - 0.02f, 0.74f, z + 0.03f, CHROME, CHROME, light);
            }
            box(buffers, pose, rear - 0.13f, 0.5f, -0.45f, rear - 0.11f, 0.54f, -0.11f, GarageSkin.RUST, GarageSkin.RUST, light);
        }
        if ((gadgets & G_SPIKES) != 0) {
            // A rolled spike strip slung under the back bumper.
            box(buffers, pose, rear - 0.16f, 0.16f, -0.42f, rear - 0.02f, 0.28f, 0.42f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            for (int k = -3; k <= 3; k++) {
                float z = k * 0.12f;
                box(buffers, pose, rear - 0.2f, 0.2f, z - 0.015f, rear - 0.14f, 0.24f, z + 0.015f, CHROME, CHROME, light);
            }
        }
        if ((gadgets & G_RACK) != 0) {
            // A roof rack: rails, cross bars, a crate and a spare tyre strapped down.
            float y = rackY;
            for (float z : new float[]{-0.44f, 0.4f}) {
                box(buffers, pose, rackX0, y, z, rackX1, y + 0.05f, z + 0.04f, GarageSkin.SOOT, GarageSkin.SOOT, light);
                box(buffers, pose, rackX0, y + 0.05f, z, rackX0 + 0.04f, y + 0.12f, z + 0.04f, GarageSkin.SOOT, GarageSkin.SOOT, light);
                box(buffers, pose, rackX1 - 0.04f, y + 0.05f, z, rackX1, y + 0.12f, z + 0.04f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            }
            float mid = (rackX0 + rackX1) / 2f;
            for (float x : new float[]{rackX0 + 0.06f, mid, rackX1 - 0.1f}) {
                box(buffers, pose, x, y + 0.02f, -0.44f, x + 0.04f, y + 0.06f, 0.44f, GarageSkin.SOOT, GarageSkin.SOOT, light);
            }
            box(buffers, pose, rackX0 + 0.06f, y + 0.06f, -0.38f, mid - 0.02f, y + 0.3f, -0.02f, GarageSkin.PLANKS, GarageSkin.PLANKS, light);
            // Load kept to the back half so the driver can still see out.
            box(buffers, pose, rackX0 + 0.06f, y + 0.06f, 0.02f, mid - 0.02f, y + 0.16f, 0.36f, GarageSkin.TYRE, GarageSkin.TYRE_TOP, light);
            box(buffers, pose, rackX0 + 0.04f, y + 0.3f, -0.22f, mid, y + 0.32f, -0.18f, GarageSkin.RUST, GarageSkin.RUST, light);
        }
        // Headlight beams: soft cones reaching ahead while the engine runs and the lights are on.
        if (lights && o.engine()) {
            VertexConsumer glow = buffers.getBuffer(GlowRenderType.GLOW);
            float pulse = 0.9f + 0.1f * Mth.sin(time * 0.7f);
            for (float z : new float[]{lampZ, -lampZ}) {
                for (int k = 1; k <= 5; k++) {
                    float d = k * 0.55f;
                    GarageControllerRenderer.softGlow(glow, pose.last().pose(), new Vec3(front + d, lampY - k * 0.03f, z * (1 + k * 0.12f)),
                            0.22f + k * 0.12f, new float[]{1f, 0.95f, 0.75f}, (0.28f - k * 0.045f) * pulse);
                }
            }
        }
    }

    // =================================================================== shared parts
    private static float flicker(float time, Options o) {
        if (!lights) {
            return 0f;
        }
        return o.engine() ? 0.75f + 0.15f * Mth.sin(time * 1.7f) + 0.1f * Mth.sin(time * 4.9f) : 0f;
    }

    private static void ram(BlockRenderDispatcher blocks, MultiBufferSource buffers, PoseStack pose, float x, int light) {
        box(buffers, pose, x, 0.3f, -0.56f, x + 0.11f, 0.45f, 0.56f, GarageSkin.RUST, GarageSkin.RUST, light);
        for (float z : new float[]{-0.4f, 0f, 0.4f}) {
            pose.pushPose();
            pose.translate(x + 0.1f, 0.375f, z);
            pose.mulPose(Axis.ZP.rotationDegrees(-90));
            pose.scale(0.4f, 0.5f, 0.4f);
            pose.translate(-0.5, 0, -0.5);
            blocks.renderSingleBlock(Blocks.POINTED_DRIPSTONE.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    /** The twin-barrel gun turret, sitting at (x, y) and pointing along yaw. */
    private static void turret(MultiBufferSource buffers, PoseStack pose, float x, float y, float yaw, int light) {
        box(buffers, pose, x - 0.2f, y, -0.2f, x + 0.2f, y + 0.08f, 0.2f, GarageSkin.SOOT, GarageSkin.RUST, light);
        pose.pushPose();
        pose.translate(x, y + 0.08f, 0);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        box(buffers, pose, -0.16f, 0f, -0.14f, 0.12f, 0.18f, 0.14f, GarageSkin.RUST, GarageSkin.SOOT, light);
        for (float z : new float[]{-0.07f, 0.04f}) {
            box(buffers, pose, 0.1f, 0.07f, z, 0.62f, 0.11f, z + 0.03f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        }
        box(buffers, pose, -0.14f, 0.18f, -0.1f, 0.04f, 0.24f, 0.1f, GarageSkin.SOOT, GarageSkin.SOOT, light);
        pose.popPose();
    }

    /** An eight-sided wheel: two square tyres turned 45 degrees to each other, with a chrome hubcap. */
    private static void wheel(MultiBufferSource buffers, PoseStack pose, float x, float r, float z, float width,
                              float spin, int light) {
        for (int k = 0; k < 2; k++) {
            pose.pushPose();
            pose.translate(x, r, z);
            pose.mulPose(Axis.ZP.rotationDegrees(45 * k - spin));
            pose.mulPose(Axis.XP.rotationDegrees(90));
            float rr = k == 0 ? r : r * 0.98f;
            box(buffers, pose, -rr, -width / 2, -rr, rr, width / 2, rr, GarageSkin.TYRE, GarageSkin.TYRE, light);
            pose.popPose();
        }
        hub(buffers, pose, x, r, z + Math.signum(z) * width / 2, r * 0.62f, Math.signum(z), spin, HUB, light);
    }

    /** A flat disc (a square, turning) on the side of a wheel, facing out along z. */
    private static void hub(MultiBufferSource buffers, PoseStack pose, float x, float y, float z, float r, float side,
                            float spin, int tile, int light) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.ZP.rotationDegrees(-spin));
        float t = 0.012f;
        box(buffers, pose, -r, -r, side > 0 ? 0 : -t, r, r, side > 0 ? t : 0, tile, tile, light);
        pose.popPose();
    }
}
