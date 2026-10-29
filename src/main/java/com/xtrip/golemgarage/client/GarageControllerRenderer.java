package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtrip.golemgarage.block.ModBlocks;
import com.xtrip.golemgarage.block.entity.GarageControllerBlockEntity;
import com.xtrip.golemgarage.entity.GolemEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * The Garage repair show: the golem rides a lift platform to the middle of the bay and turns
 * slowly while every ceiling emitter fires a pulsing beam that sweeps up and down it, and a
 * scanning ring climbs its body.
 */
public class GarageControllerRenderer implements BlockEntityRenderer<GarageControllerBlockEntity> {
    private final EntityRenderDispatcher entities;
    private final BlockRenderDispatcher blocks;
    private final net.minecraft.client.gui.Font font;

    public GarageControllerRenderer(BlockEntityRendererProvider.Context context) {
        this.entities = context.getEntityRenderer();
        this.blocks = context.getBlockRenderDispatcher();
        this.font = context.getFont();
    }

    @Override
    public void render(GarageControllerBlockEntity garage, float partial, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = garage.getLevel();
        if (level == null) {
            return;
        }
        BlockPos origin = garage.getBlockPos();
        float time = level.getGameTime() + partial;
        if (garage.isFormed()) {
            renderFormed(garage, time, pose, buffers);
        }
        BlockPos center = garage.padCenter();
        if (center == null || (!garage.isBusy() && !garage.isFormed())) {
            return;
        }
        float lift = garage.isBusy() ? garage.liftHeight(partial) : 0f;
        double cx = center.getX() + 0.5 - origin.getX();
        double cy = center.getY() - origin.getY();
        double cz = center.getZ() + 0.5 - origin.getZ();
        int lightAt = LevelRenderer.getLightColor(level, center.above());

        // Lift platform with a hazard-striped ring around it.
        pose.pushPose();
        pose.translate(cx - 0.8, cy + lift - 0.0625, cz - 0.8);
        pose.scale(1.6f, 0.1875f, 1.6f);
        blocks.renderSingleBlock(Blocks.POLISHED_DEEPSLATE.defaultBlockState(), pose, buffers, lightAt, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.pushPose();
        pose.translate(cx - 0.3, cy + lift + 0.125, cz - 0.3);
        pose.scale(0.6f, 0.01f, 0.6f);
        blocks.renderSingleBlock(ModBlocks.SIGNAL_PLATE.get().defaultBlockState(), pose, buffers, lightAt, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        hazardRing(pose, buffers, cx, cy, cz, lightAt);

        if (!garage.isBusy()) {
            return;
        }
        // The golem itself, slowly turning on the platform.
        GolemEntity golem = garage.displayGolem();
        if (golem != null) {
            float yaw = (time * 0.6f) % 360f;
            golem.setPos(center.getX() + 0.5, center.getY() + lift + 0.125, center.getZ() + 0.5);
            golem.tickCount = (int) level.getGameTime();
            golem.setYRot(yaw);
            golem.yRotO = yaw;
            golem.yBodyRot = yaw;
            golem.yBodyRotO = yaw;
            golem.yHeadRot = yaw;
            golem.yHeadRotO = yaw;
            golem.setXRot(0);
            entities.render(golem, cx, cy + lift + 0.125, cz, yaw, partial, pose, buffers, lightAt);
        }

        // Beams and the scanning ring, faded in and out with the lift.
        float fade = Mth.clamp(lift / 0.6f, 0f, 1f);
        if (fade <= 0.01f) {
            return;
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        List<BlockPos> emitters = garage.getEmitters();
        for (int i = 0; i < emitters.size(); i++) {
            BlockPos e = emitters.get(i);
            Vec3 from = new Vec3(e.getX() + 0.5 - origin.getX(), e.getY() + 0.47 - origin.getY(), e.getZ() + 0.5 - origin.getZ());
            Vec3 hit = garage.beamTarget(i, time);
            Vec3 to = new Vec3(hit.x - origin.getX(), hit.y - origin.getY(), hit.z - origin.getZ());
            boolean steel = level.getBlockState(e).is(ModBlocks.STEEL_EMITTER.get());
            float pulse = 0.75f + 0.25f * Mth.sin(time * 0.6f + i);
            float r = steel ? 1.0f : 0.35f;
            float g = steel ? 0.72f : 0.9f;
            float b = steel ? 0.25f : 1.0f;
            beam(vc, m, from, to, 0.11f, r, g, b, 0.28f * fade * pulse);
            beam(vc, m, from, to, 0.04f, 0.9f + r * 0.1f, 0.95f, 1.0f, 0.85f * fade * pulse);
        }

        double base = cy + lift + 0.125;
        double ringY = base + 0.15 + (Mth.sin(time * 0.05f) * 0.5f + 0.5f) * 2.5;
        ring(vc, m, cx, ringY, cz, 0.85, 0.35f, 0.9f, 1.0f, 0.45f * fade);
        ring(vc, m, cx, ringY, cz, 0.9, 0.35f, 0.9f, 1.0f, 0.2f * fade);
    }

    // =================================================================== the finished Garage
    private static final int FULL_BRIGHT = 0xF000F0;

    /** Floor markings, the neon sign outside, the status panel inside and, during a repair, warning lights. */
    private void renderFormed(GarageControllerBlockEntity garage, float time, PoseStack pose, MultiBufferSource buffers) {
        Level level = garage.getLevel();
        BlockPos origin = garage.getBlockPos();
        BlockPos min = garage.getBayMin();
        BlockPos max = garage.getBayMax();
        if (min != null && max != null) {
            int floorLight = LevelRenderer.getLightColor(level, min);
            float y = min.getY() - origin.getY();
            float x0 = min.getX() - origin.getX() + 0.18f, x1 = max.getX() + 1 - origin.getX() - 0.18f;
            float z0 = min.getZ() - origin.getZ() + 0.18f, z1 = max.getZ() + 1 - origin.getZ() - 0.18f;
            float w = 0.09f;
            BlockState yellow = Blocks.YELLOW_CONCRETE.defaultBlockState();
            slab(pose, buffers, yellow, x0, y, z0, x1, z0 + w, floorLight);
            slab(pose, buffers, yellow, x0, y, z1 - w, x1, z1, floorLight);
            slab(pose, buffers, yellow, x0, y, z0, x0 + w, z1, floorLight);
            slab(pose, buffers, yellow, x1 - w, y, z0, x1, z1, floorLight);
        }

        Direction out = garage.outside();
        Direction in = out.getOpposite();
        long formedFor = garage.formedFor((long) time);
        boolean flickerOn = formedFor > 40 || ((formedFor * 7919L) >> 2) % 5 < 2;
        boolean busy = garage.isBusy();
        int pct = Math.round(100f * garage.getProgress() / Math.max(1, garage.getRequired()));

        // Outside: a neon sign on the wall above the controller.
        pose.pushPose();
        faceWall(pose, out, 2.05f);
        slabXY(pose, buffers, Blocks.BLACK_CONCRETE.defaultBlockState(), -1.45f, -0.5f, 1.45f, 0.5f, 0.05f);
        float neon = flickerOn ? 1f : 0.15f;
        frame(buffers.getBuffer(RenderType.lightning()), pose.last().pose(), -1.4f, -0.45f, 1.4f, 0.45f, 0.06f, 0.035f, 0.35f * neon, 0.9f * neon, 1f * neon, 0.9f);
        text(pose, buffers, Component.translatable("golemgarage.sign.title"), 0f, 0.1f, 2.5f, 0.06f,
                flickerOn ? 0x9CF6FF : 0x1B4650, flickerOn ? 0x0E6A86 : 0x0A1A20);
        Component status = busy ? Component.translatable("golemgarage.sign.repairing", pct)
                : Component.translatable("golemgarage.sign.open");
        boolean blink = !busy || ((int) (time / 10)) % 2 == 0;
        text(pose, buffers, status, 0f, -0.25f, 1.2f, 0.06f,
                busy ? (blink ? 0xFFC04A : 0xA0701C) : 0x7CFF8A, busy ? 0x6A3A00 : 0x0E5A1A);
        if (busy) {
            beacon(pose, buffers, 0f, 0.62f, 0.12f, time);
        }
        pose.popPose();

        // Inside: a status panel above the controller, readable through the windows too.
        pose.pushPose();
        faceWall(pose, in, 1.55f);
        slabXY(pose, buffers, Blocks.BLACK_CONCRETE.defaultBlockState(), -0.7f, -0.28f, 0.7f, 0.28f, 0.04f);
        Matrix4f m = pose.last().pose();
        VertexConsumer glow2 = buffers.getBuffer(RenderType.lightning());
        frame(glow2, m, -0.66f, -0.24f, 0.66f, 0.24f, 0.05f, 0.025f, 0.35f, 0.9f, 1f, 0.7f);
        float fill = busy ? Mth.clamp(pct / 100f, 0f, 1f) : 1f;
        float r = busy ? 1f : 0.3f, g = busy ? 0.7f : 1f, b = busy ? 0.2f : 0.45f;
        rect(glow2, m, -0.58f, -0.16f, -0.58f + 1.16f * fill, -0.04f, 0.052f, r, g, b, busy ? 0.75f : 0.35f);
        text(pose, buffers, busy ? Component.literal(pct + "%") : Component.translatable("golemgarage.sign.ready"),
                0f, 0.12f, 0.5f, 0.055f, busy ? 0xFFC04A : 0x7CFF8A, 0x10202A);
        if (busy) {
            beacon(pose, buffers, 0f, 1.05f, 0.12f, time + 20);
        }
        pose.popPose();
    }

    /** Moves the pose onto the outer face of the wall block above the controller, looking out along dir. */
    private static void faceWall(PoseStack pose, Direction dir, float height) {
        pose.translate(0.5, height, 0.5);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-dir.toYRot()));
        pose.translate(0, 0, 0.5);
    }

    /** Neon text centred at (x, y), scaled to the given width, lying just off the wall. */
    private void text(PoseStack pose, MultiBufferSource buffers, Component text, float x, float y, float width, float z,
                      int color, int outline) {
        FormattedCharSequence seq = text.getVisualOrderText();
        int px = Math.max(1, font.width(seq));
        float scale = Math.min(width / px, 0.035f);
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(scale, -scale, scale);
        font.drawInBatch8xOutline(seq, -px / 2f, -4f, color, outline, pose.last().pose(), buffers, FULL_BRIGHT);
        pose.popPose();
    }

    /** A spinning amber warning light on a small housing at (x, y) on the wall. */
    private void beacon(PoseStack pose, MultiBufferSource buffers, float x, float y, float z, float time) {
        pose.pushPose();
        pose.translate(x - 0.1, y - 0.1, 0);
        pose.scale(0.2f, 0.2f, 0.2f);
        blocks.renderSingleBlock(Blocks.ORANGE_STAINED_GLASS.defaultBlockState(), pose, buffers, FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        Matrix4f m = pose.last().pose();
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        Vec3 c = new Vec3(x, y, z);
        float a = time * 0.22f;
        for (int i = 0; i < 2; i++) {
            double ang = a + i * Math.PI;
            Vec3 tip = c.add(Math.cos(ang) * 1.3, 0, Math.sin(ang) * 1.3);
            beam(glow, m, c, tip, 0.09f, 1f, 0.55f, 0.1f, 0.35f);
        }
        rect(glow, m, x - 0.16f, y - 0.16f, x + 0.16f, y + 0.16f, z + 0.11f, 1f, 0.6f, 0.15f,
                0.25f + 0.2f * Mth.sin(time * 0.5f));
    }

    /** A thin flat slab of a block on the floor, from (x0, z0) to (x1, z1). */
    private void slab(PoseStack pose, MultiBufferSource buffers, BlockState state, float x0, float y, float z0,
                      float x1, float z1, int light) {
        pose.pushPose();
        pose.translate(x0, y + 0.002f, z0);
        pose.scale(x1 - x0, 0.012f, z1 - z0);
        blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** A thin board of a block lying flat against the wall (in the wall's own x/y), depth d. */
    private void slabXY(PoseStack pose, MultiBufferSource buffers, BlockState state, float x0, float y0, float x1, float y1, float d) {
        pose.pushPose();
        pose.translate(x0, y0, 0.002f);
        pose.scale(x1 - x0, y1 - y0, d);
        blocks.renderSingleBlock(state, pose, buffers, FULL_BRIGHT - 0x500000, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private void hazardRing(PoseStack pose, MultiBufferSource buffers, double cx, double cy, double cz, int light) {
        BlockState yellow = Blocks.YELLOW_CONCRETE.defaultBlockState();
        BlockState black = Blocks.BLACK_CONCRETE.defaultBlockState();
        float in = 0.84f, outer = 1.04f, seg = 0.26f;
        int n = Math.round(2 * outer / seg);
        float y = (float) cy;
        for (int i = 0; i < n; i++) {
            float a = (float) (-outer + i * seg);
            float b = a + seg;
            BlockState s = i % 2 == 0 ? yellow : black;
            slab(pose, buffers, s, (float) cx + a, y, (float) cz - outer, (float) cx + b, (float) cz - in, light);
            slab(pose, buffers, s, (float) cx + a, y, (float) cz + in, (float) cx + b, (float) cz + outer, light);
            slab(pose, buffers, s, (float) cx - outer, y, (float) cz + a, (float) cx - in, (float) cz + b, light);
            slab(pose, buffers, s, (float) cx + in, y, (float) cz + a, (float) cx + outer, (float) cz + b, light);
        }
    }

    /** A glowing rectangle in the wall plane at depth z. */
    private static void rect(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z,
                             float r, float g, float b, float a) {
        vc.addVertex(m, x0, y0, z).setColor(r, g, b, a);
        vc.addVertex(m, x1, y0, z).setColor(r, g, b, a);
        vc.addVertex(m, x1, y1, z).setColor(r, g, b, a);
        vc.addVertex(m, x0, y1, z).setColor(r, g, b, a);
        vc.addVertex(m, x0, y1, z).setColor(r, g, b, a);
        vc.addVertex(m, x1, y1, z).setColor(r, g, b, a);
        vc.addVertex(m, x1, y0, z).setColor(r, g, b, a);
        vc.addVertex(m, x0, y0, z).setColor(r, g, b, a);
    }

    /** A glowing outline in the wall plane. */
    private static void frame(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, float t,
                              float r, float g, float b, float a) {
        rect(vc, m, x0, y0, x1, y0 + t, z, r, g, b, a);
        rect(vc, m, x0, y1 - t, x1, y1, z, r, g, b, a);
        rect(vc, m, x0, y0 + t, x0 + t, y1 - t, z, r, g, b, a);
        rect(vc, m, x1 - t, y0 + t, x1, y1 - t, z, r, g, b, a);
    }

    /** A beam made of two crossed quads, drawn from both sides. */
    private static void beam(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float width,
                             float r, float g, float bl, float alpha) {
        Vector3f dir = new Vector3f((float) (b.x - a.x), (float) (b.y - a.y), (float) (b.z - a.z));
        if (dir.lengthSquared() < 1e-6) {
            return;
        }
        Vector3f side = new Vector3f(dir).cross(0, 1, 0);
        if (side.lengthSquared() < 1e-6) {
            side.set(1, 0, 0);
        }
        side.normalize(width);
        Vector3f up = new Vector3f(dir).cross(side).normalize(width);
        quad(vc, m, a, b, side, r, g, bl, alpha);
        quad(vc, m, a, b, up, r, g, bl, alpha);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vector3f off,
                             float r, float g, float bl, float alpha) {
        float ax = (float) a.x, ay = (float) a.y, az = (float) a.z;
        float bx = (float) b.x, by = (float) b.y, bz = (float) b.z;
        // Front face.
        vc.addVertex(m, ax - off.x, ay - off.y, az - off.z).setColor(r, g, bl, alpha);
        vc.addVertex(m, ax + off.x, ay + off.y, az + off.z).setColor(r, g, bl, alpha);
        vc.addVertex(m, bx + off.x, by + off.y, bz + off.z).setColor(r, g, bl, alpha);
        vc.addVertex(m, bx - off.x, by - off.y, bz - off.z).setColor(r, g, bl, alpha);
        // Back face.
        vc.addVertex(m, bx - off.x, by - off.y, bz - off.z).setColor(r, g, bl, alpha);
        vc.addVertex(m, bx + off.x, by + off.y, bz + off.z).setColor(r, g, bl, alpha);
        vc.addVertex(m, ax + off.x, ay + off.y, az + off.z).setColor(r, g, bl, alpha);
        vc.addVertex(m, ax - off.x, ay - off.y, az - off.z).setColor(r, g, bl, alpha);
    }

    /** A thin square ring around the golem at height y. */
    private static void ring(VertexConsumer vc, Matrix4f m, double cx, double y, double cz, double half,
                             float r, float g, float b, float alpha) {
        float t = 0.05f;
        float x0 = (float) (cx - half), x1 = (float) (cx + half);
        float z0 = (float) (cz - half), z1 = (float) (cz + half);
        float yy = (float) y;
        band(vc, m, x0, z0, x1, z0 + t, yy, r, g, b, alpha);
        band(vc, m, x0, z1 - t, x1, z1, yy, r, g, b, alpha);
        band(vc, m, x0, z0, x0 + t, z1, yy, r, g, b, alpha);
        band(vc, m, x1 - t, z0, x1, z1, yy, r, g, b, alpha);
    }

    private static void band(VertexConsumer vc, Matrix4f m, float x0, float z0, float x1, float z1, float y,
                             float r, float g, float b, float a) {
        vc.addVertex(m, x0, y, z0).setColor(r, g, b, a);
        vc.addVertex(m, x0, y, z1).setColor(r, g, b, a);
        vc.addVertex(m, x1, y, z1).setColor(r, g, b, a);
        vc.addVertex(m, x1, y, z0).setColor(r, g, b, a);
        vc.addVertex(m, x1, y, z0).setColor(r, g, b, a);
        vc.addVertex(m, x1, y, z1).setColor(r, g, b, a);
        vc.addVertex(m, x0, y, z1).setColor(r, g, b, a);
        vc.addVertex(m, x0, y, z0).setColor(r, g, b, a);
    }

    @Override
    public boolean shouldRenderOffScreen(GarageControllerBlockEntity garage) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(GarageControllerBlockEntity garage) {
        if (garage.getBayMin() != null && garage.getBayMax() != null) {
            return new AABB(Vec3.atLowerCornerOf(garage.getBayMin()), Vec3.atLowerCornerOf(garage.getBayMax()).add(1, 1, 1))
                    .minmax(new AABB(garage.getBlockPos())).inflate(3);
        }
        return new AABB(garage.getBlockPos()).inflate(10);
    }
}
