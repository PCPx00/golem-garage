package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.block.GarageDoorBlock;
import com.xtrip.golemgarage.block.entity.GarageDoorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws a roller door: steel slats that roll up into a drum housing at the top of the door, with guide rails
 * down the sides. Each block draws its own share of the curtain, so a door several blocks tall rolls up as one.
 */
public class GarageDoorRenderer implements BlockEntityRenderer<GarageDoorBlockEntity> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "textures/entity/garage_door.png");
    private static final float TW = 64f;
    private static final float TH = 32f;
    private static final float SLAT = 0.25f;
    private static final float Z0 = 0.45f;
    private static final float Z1 = 0.55f;

    public GarageDoorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GarageDoorBlockEntity door, float partial, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = door.getLevel();
        BlockState state = door.getBlockState();
        if (level == null || !(state.getBlock() instanceof GarageDoorBlock)) {
            return;
        }
        BlockPos pos = door.getBlockPos();
        Direction facing = state.getValue(GarageDoorBlock.FACING);
        Direction across = GarageDoorBlockEntity.across(state);

        // This block's column of the door.
        int bottom = pos.getY();
        while (bottom > pos.getY() - 16 && same(level, state, pos.atY(bottom - 1))) {
            bottom--;
        }
        int top = pos.getY();
        while (top < pos.getY() + 16 && same(level, state, pos.atY(top + 1))) {
            top++;
        }
        int height = top - bottom + 1;
        // The bottom column block knows the roll for the whole column.
        float rolled = door.rolled(partial);
        if (bottom != pos.getY() && level.getBlockEntity(pos.atY(bottom)) instanceof GarageDoorBlockEntity base) {
            rolled = base.rolled(partial);
        }
        float eased = rolled * rolled * (3 - 2 * rolled);
        float edge = bottom + eased * (height - 0.18f) - pos.getY();

        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(-0.5, 0, -0.5);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        PoseStack.Pose p = pose.last();

        // The slat curtain, from its bottom edge up to the top of this block.
        float from = Math.max(0f, edge);
        if (from < 1f) {
            int first = Mth.floor((from - edge) / SLAT);
            int last = Mth.ceil((1f - edge) / SLAT);
            for (int k = Math.max(0, first); k < last; k++) {
                float s0 = edge + k * SLAT;
                float s1 = s0 + SLAT;
                float y0 = Math.max(s0, from);
                float y1 = Math.min(s1, 1f);
                if (y1 <= y0) {
                    continue;
                }
                float vTop = k == 0 ? 4f : 0f;
                float v0 = vTop + 4f * (1f - (y0 - s0) / SLAT);
                float v1 = vTop + 4f * (1f - (y1 - s0) / SLAT);
                face(vc, p, 0, y0, Z1, 1, y1, Z1, 0, 0, 1, 0, v0, 16, v1, light, overlay);
                face(vc, p, 1, y0, Z0, 0, y1, Z0, 0, 0, -1, 0, v0, 16, v1, light, overlay);
            }
            if (edge >= 0f && edge < 1f) {
                // Rubber seal along the bottom edge.
                bottomFace(vc, p, edge, light, overlay);
            }
        }

        // Guide rails down the open sides of the door.
        if (!same(level, state, pos.relative(across))) {
            box(vc, p, -0.02f, 0, 0.36f, 0.07f, 1, 0.64f, 32, 0, 36, 16, light, overlay);
        }
        if (!same(level, state, pos.relative(across.getOpposite()))) {
            box(vc, p, 0.93f, 0, 0.36f, 1.02f, 1, 0.64f, 32, 0, 36, 16, light, overlay);
        }
        // The drum housing on top, on the inside of the door.
        if (top == pos.getY()) {
            box(vc, p, 0, 0.7f, 0.1f, 1, 1, 0.45f, 16, 0, 32, 8, light, overlay);
            box(vc, p, 0, 0.66f, 0.42f, 1, 1, 0.6f, 16, 8, 32, 16, light, overlay);
        }
        pose.popPose();
    }

    private static boolean same(Level level, BlockState state, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getBlock() instanceof GarageDoorBlock && s.getValue(GarageDoorBlock.FACING) == state.getValue(GarageDoorBlock.FACING);
    }

    /** A vertical quad from (x0,y0,z0) to (x1,y1,z1), textured with a pixel rectangle. */
    private static void face(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1,
                             float nx, float ny, float nz, float u0, float v0, float u1, float v1, int light, int overlay) {
        vert(vc, p, x0, y0, z0, u0, v0, nx, ny, nz, light, overlay);
        vert(vc, p, x1, y0, z1, u1, v0, nx, ny, nz, light, overlay);
        vert(vc, p, x1, y1, z1, u1, v1, nx, ny, nz, light, overlay);
        vert(vc, p, x0, y1, z0, u0, v1, nx, ny, nz, light, overlay);
    }

    private static void bottomFace(VertexConsumer vc, PoseStack.Pose p, float y, int light, int overlay) {
        vert(vc, p, 0, y, Z0, 0, 7, 0, -1, 0, light, overlay);
        vert(vc, p, 1, y, Z0, 16, 7, 0, -1, 0, light, overlay);
        vert(vc, p, 1, y, Z1, 16, 8, 0, -1, 0, light, overlay);
        vert(vc, p, 0, y, Z1, 0, 8, 0, -1, 0, light, overlay);
    }

    /** A box with the same pixel rectangle on every face. */
    private static void box(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1,
                            float u0, float v0, float u1, float v1, int light, int overlay) {
        // south (+z) and north (-z)
        face(vc, p, x0, y0, z1, x1, y1, z1, 0, 0, 1, u0, v1, u1, v0, light, overlay);
        face(vc, p, x1, y0, z0, x0, y1, z0, 0, 0, -1, u0, v1, u1, v0, light, overlay);
        // east (+x) and west (-x)
        face(vc, p, x1, y0, z1, x1, y1, z0, 1, 0, 0, u0, v1, u1, v0, light, overlay);
        face(vc, p, x0, y0, z0, x0, y1, z1, -1, 0, 0, u0, v1, u1, v0, light, overlay);
        // top and bottom
        vert(vc, p, x0, y1, z1, u0, v1, 0, 1, 0, light, overlay);
        vert(vc, p, x1, y1, z1, u1, v1, 0, 1, 0, light, overlay);
        vert(vc, p, x1, y1, z0, u1, v0, 0, 1, 0, light, overlay);
        vert(vc, p, x0, y1, z0, u0, v0, 0, 1, 0, light, overlay);
        vert(vc, p, x0, y0, z0, u0, v0, 0, -1, 0, light, overlay);
        vert(vc, p, x1, y0, z0, u1, v0, 0, -1, 0, light, overlay);
        vert(vc, p, x1, y0, z1, u1, v1, 0, -1, 0, light, overlay);
        vert(vc, p, x0, y0, z1, u0, v1, 0, -1, 0, light, overlay);
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v,
                             float nx, float ny, float nz, int light, int overlay) {
        vc.addVertex(p, x, y, z).setColor(255, 255, 255, 255).setUv(u / TW, v / TH)
                .setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
