package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.item.GolemLinkerItem;
import com.xtrip.golemgarage.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * While holding a Golem Linker: outlines what nearby golems are linked to (post, chests, Garage, work area),
 * with a line from each golem to its links, and the Linker's own marks pulsing.
 */
@EventBusSubscriber(modid = GolemGarage.MOD_ID, value = Dist.CLIENT)
public final class LinkOverlay {
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        ItemStack held = mc.player.getMainHandItem().is(ModItems.GOLEM_LINKER.get()) ? mc.player.getMainHandItem()
                : mc.player.getOffhandItem().is(ModItems.GOLEM_LINKER.get()) ? mc.player.getOffhandItem() : null;
        if (held == null) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        float t = (System.currentTimeMillis() % 1200L) / 1200f;
        float pulse = 0.55f + 0.45f * (float) Math.sin(t * Math.PI * 2);

        for (GolemEntity g : mc.level.getEntitiesOfClass(GolemEntity.class, mc.player.getBoundingBox().inflate(40))) {
            if (g.getOwnerUuid() != null && !g.getOwnerUuid().equals(mc.player.getUUID())) {
                continue;
            }
            BlockPos a1 = g.readSyncedLink(GolemLinkerItem.Mode.AREA_1);
            BlockPos a2 = g.readSyncedLink(GolemLinkerItem.Mode.AREA_2);
            if (a1 != null && a2 != null) {
                AABB area = new AABB(Math.min(a1.getX(), a2.getX()), Math.min(a1.getY(), a2.getY()),
                        Math.min(a1.getZ(), a2.getZ()), Math.max(a1.getX(), a2.getX()) + 1,
                        Math.max(a1.getY(), a2.getY()) + 1, Math.max(a1.getZ(), a2.getZ()) + 1);
                box(pose, lines, area, 0xD98CFF, 0.9f);
            }
            for (GolemLinkerItem.Mode m : GolemLinkerItem.Mode.MARKS) {
                BlockPos p = g.readSyncedLink(m);
                if (p == null || m == GolemLinkerItem.Mode.AREA_1 || m == GolemLinkerItem.Mode.AREA_2) {
                    continue;
                }
                box(pose, lines, new AABB(p).inflate(0.02), m.color, 0.95f);
                Vec3 from = g.position().add(0, g.getBbHeight() * 0.6, 0);
                Vec3 to = Vec3.atCenterOf(p);
                line(pose, lines, from, to, m.color, 0.6f);
            }
        }
        // The Linker's own marks, pulsing.
        for (GolemLinkerItem.Mode m : GolemLinkerItem.Mode.MARKS) {
            BlockPos p = GolemLinkerItem.mark(held, m);
            if (p != null) {
                box(pose, lines, new AABB(p).inflate(0.06 + 0.04 * pulse), m.color, pulse);
            }
        }
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    private static void box(PoseStack pose, VertexConsumer c, AABB b, int col, float a) {
        LevelRenderer.renderLineBox(pose, c, b, ((col >> 16) & 255) / 255f, ((col >> 8) & 255) / 255f, (col & 255) / 255f, a);
    }

    private static void line(PoseStack pose, VertexConsumer c, Vec3 a, Vec3 b, int col, float alpha) {
        float r = ((col >> 16) & 255) / 255f;
        float g = ((col >> 8) & 255) / 255f;
        float bl = (col & 255) / 255f;
        Vec3 n = b.subtract(a).normalize();
        PoseStack.Pose last = pose.last();
        c.addVertex(last, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, alpha).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
        c.addVertex(last, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, alpha).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
    }

    private LinkOverlay() {
    }
}
