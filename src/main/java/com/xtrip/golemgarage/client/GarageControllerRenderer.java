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

    public GarageControllerRenderer(BlockEntityRendererProvider.Context context) {
        this.entities = context.getEntityRenderer();
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(GarageControllerBlockEntity garage, float partial, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = garage.getLevel();
        BlockPos center = garage.getBayCenter();
        if (!garage.isBusy() || center == null || level == null) {
            return;
        }
        BlockPos origin = garage.getBlockPos();
        float time = level.getGameTime() + partial;
        float lift = garage.liftHeight(partial);
        double cx = center.getX() + 0.5 - origin.getX();
        double cy = center.getY() - origin.getY();
        double cz = center.getZ() + 0.5 - origin.getZ();
        int lightAt = LevelRenderer.getLightColor(level, center.above());

        // Lift platform.
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
        return new AABB(garage.getBlockPos()).inflate(10);
    }
}
