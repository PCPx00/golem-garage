package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.block.GarageControllerBlock;
import com.xtrip.golemgarage.block.GarageDoorBlock;
import com.xtrip.golemgarage.block.LaserEmitterBlock;
import com.xtrip.golemgarage.block.entity.GarageControllerBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The wasteland look a finished Garage turns into. Like a formed multiblock, the room's own blocks disappear
 * behind a new skin: rusty corrugated walls, grimy brick and bolted plates inside, a plank roof with a hoist beam,
 * an oily concrete floor, and a weathered shell outside with spikes along the roof and a smoking exhaust stack.
 * Props fill the corners: a workbench, a stack of tyres, oil drums and jerry cans. The panels sweep into place
 * from the controller outwards when the room is completed. Nothing in the world changes: break a block and the
 * room goes back to how it was built.
 */
final class GarageSkin {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "textures/entity/garage_skin.png");

    // Tile numbers in garage_skin.png (see tools/golemgen/skin.py).
    static final int CORRUGATED = 0, BRICK = 1, PLATES = 2, KICK = 3, PLANKS = 4, BEAM_PLANKS = 5, CONCRETE = 6,
            GRATE = 7, OUTER = 8, OUTER_HAZARD = 9, ROOF = 10, TYRE = 11, TYRE_TOP = 12, RUST = 13, JERRY = 14,
            BENCH = 15, SIGN = 16, DRUM = 17, DRUM_TOP = 18, SOOT = 19;

    private static final float EPS = 0.004f;
    private static final int REBUILD_TICKS = 20;
    private static final float PANEL_TICKS = 14f;
    private static final float WAVE = 4.5f;

    private record Face(int x, int y, int z, Direction dir, int tile, int light, float delay) {
    }

    private static final class Cache {
        long builtAt;
        BlockPos min;
        BlockPos max;
        final List<Face> faces = new ArrayList<>();
    }

    private static final Map<GarageControllerBlockEntity, Cache> CACHE = new WeakHashMap<>();

    private GarageSkin() {
    }

    /** Ticks since forming at which the whole skin is in place and props appear. */
    static float propsAt(GarageControllerBlockEntity garage) {
        Cache c = CACHE.get(garage);
        float last = 0;
        if (c != null) {
            for (Face f : c.faces) {
                last = Math.max(last, f.delay);
            }
        }
        return last + PANEL_TICKS;
    }

    // =================================================================== building the face list
    private static Cache cache(GarageControllerBlockEntity garage, Level level) {
        Cache c = CACHE.computeIfAbsent(garage, g -> new Cache());
        long now = level.getGameTime();
        if (c.faces.isEmpty() || now - c.builtAt >= REBUILD_TICKS || now < c.builtAt
                || !garage.getBayMin().equals(c.min) || !garage.getBayMax().equals(c.max)) {
            build(c, garage, level);
            c.builtAt = now;
        }
        return c;
    }

    private static boolean inside(BlockPos p, BlockPos min, BlockPos max) {
        return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY()
                && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
    }

    /** Blocks that keep their own look: the controller, emitters, doors, and anything see-through. */
    private static boolean skinnable(Level level, BlockPos p, BlockState s) {
        return s.isSolidRender(level, p) && !(s.getBlock() instanceof GarageControllerBlock)
                && !(s.getBlock() instanceof LaserEmitterBlock) && !(s.getBlock() instanceof GarageDoorBlock);
    }

    private static void build(Cache c, GarageControllerBlockEntity garage, Level level) {
        c.faces.clear();
        BlockPos min = garage.getBayMin();
        BlockPos max = garage.getBayMax();
        c.min = min;
        c.max = max;
        BlockPos sMin = min.offset(-1, -1, -1);
        BlockPos sMax = max.offset(1, 1, 1);
        BlockPos origin = garage.getBlockPos();
        int midX = (min.getX() + max.getX()) / 2;
        int midZ = (min.getZ() + max.getZ()) / 2;
        BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
        for (BlockPos p : BlockPos.betweenClosed(sMin, sMax)) {
            if (inside(p, min, max)) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            if (!skinnable(level, p, s)) {
                continue;
            }
            int h = Mth.murmurHash3Mixer(p.getX() * 73856093 ^ p.getY() * 19349663 ^ p.getZ() * 83492791) & 0x7fffffff;
            float delay = (float) Math.sqrt(p.distSqr(origin)) * WAVE;
            for (Direction d : Direction.values()) {
                n.setWithOffset(p, d);
                int tile;
                if (inside(n, min, max)) {
                    tile = switch (d) {
                        case UP -> h % 5 == 0 ? GRATE : CONCRETE;
                        case DOWN -> p.getX() == midX || p.getZ() == midZ ? BEAM_PLANKS : PLANKS;
                        default -> p.getY() == min.getY() ? KICK : new int[]{CORRUGATED, BRICK, PLATES, CORRUGATED}[h % 4];
                    };
                } else if (!inside(n, sMin, sMax) || !level.getBlockState(n).isSolidRender(level, n)) {
                    if (inside(n, sMin, sMax) && !level.getBlockState(n).isAir()) {
                        // The sides of a window or doorway: a rusty frame.
                        c.faces.add(new Face(p.getX() - origin.getX(), p.getY() - origin.getY(), p.getZ() - origin.getZ(),
                                d, RUST, LevelRenderer.getLightColor(level, n), delay));
                        continue;
                    }
                    if (d == Direction.DOWN) {
                        continue;
                    }
                    tile = d == Direction.UP ? ROOF : h % 4 == 0 ? OUTER_HAZARD : OUTER;
                } else {
                    continue;
                }
                c.faces.add(new Face(p.getX() - origin.getX(), p.getY() - origin.getY(), p.getZ() - origin.getZ(),
                        d, tile, LevelRenderer.getLightColor(level, n), delay));
            }
        }
    }

    // =================================================================== drawing
    static void render(GarageControllerBlockEntity garage, BlockRenderDispatcher blocks, float time, PoseStack pose,
                       MultiBufferSource buffers) {
        Level level = garage.getLevel();
        if (level == null || garage.getBayMin() == null || garage.getBayMax() == null) {
            return;
        }
        Cache c = cache(garage, level);
        float since = garage.formedFor((long) time) + (time - (long) time);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        PoseStack.Pose p = pose.last();
        for (Face f : c.faces) {
            float grow = Mth.clamp((since - f.delay) / PANEL_TICKS, 0f, 1f);
            if (grow <= 0f) {
                continue;
            }
            // Panels fly in from a little way off and grow to full size as they land.
            float e = easeOut(grow);
            float off = (1 - e) * 0.9f;
            face(vc, p, f.x + 0.5f + f.dir.getStepX() * off, f.y + 0.5f + f.dir.getStepY() * off,
                    f.z + 0.5f + f.dir.getStepZ() * off, f.dir, 0.5f * e, f.tile, f.light);
        }
        float props = Mth.clamp((since - propsAt(garage)) / 10f, 0f, 1f);
        if (props > 0f) {
            props(garage, blocks, time, easeOut(props), pose, buffers);
        }
    }

    private static float easeOut(float t) {
        float u = 1 - t;
        return 1 - u * u * u;
    }

    /** One square panel on the given side of a block centred at (cx, cy, cz). */
    private static void face(VertexConsumer vc, PoseStack.Pose p, float cx, float cy, float cz, Direction d, float half,
                             int tile, int light) {
        Vector3f n = new Vector3f(d.getStepX(), d.getStepY(), d.getStepZ());
        Vector3f r;
        Vector3f u;
        if (d.getAxis() == Direction.Axis.Y) {
            r = new Vector3f(1, 0, 0);
            u = new Vector3f(0, 0, d == Direction.UP ? -1 : 1);
        } else {
            Direction right = d.getCounterClockWise();
            r = new Vector3f(right.getStepX(), 0, right.getStepZ());
            u = new Vector3f(0, 1, 0);
        }
        float ox = cx + n.x * (0.5f + EPS), oy = cy + n.y * (0.5f + EPS), oz = cz + n.z * (0.5f + EPS);
        quad(vc, p, ox, oy, oz, r, u, half, half, n, tile, light);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, float ox, float oy, float oz, Vector3f r, Vector3f u,
                             float hr, float hu, Vector3f n, int tile, int light) {
        float u0 = (tile % 8) * 16 / 128f, v0 = (tile / 8) * 16 / 64f;
        float u1 = u0 + 16 / 128f, v1 = v0 + 16 / 64f;
        vert(vc, p, ox - r.x * hr - u.x * hu, oy - r.y * hr - u.y * hu, oz - r.z * hr - u.z * hu, u0, v1, n, light);
        vert(vc, p, ox + r.x * hr - u.x * hu, oy + r.y * hr - u.y * hu, oz + r.z * hr - u.z * hu, u1, v1, n, light);
        vert(vc, p, ox + r.x * hr + u.x * hu, oy + r.y * hr + u.y * hu, oz + r.z * hr + u.z * hu, u1, v0, n, light);
        vert(vc, p, ox - r.x * hr + u.x * hu, oy - r.y * hr + u.y * hu, oz - r.z * hr + u.z * hu, u0, v0, n, light);
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v,
                             Vector3f n, int light) {
        vc.addVertex(p, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(p, n.x, n.y, n.z);
    }

    /** A box from (x0,y0,z0) to (x1,y1,z1) with one tile on the sides and another on top and bottom. */
    static void box(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1,
                    int side, int top, int light) {
        float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, cz = (z0 + z1) / 2;
        float hx = (x1 - x0) / 2, hy = (y1 - y0) / 2, hz = (z1 - z0) / 2;
        Vector3f X = new Vector3f(1, 0, 0), Y = new Vector3f(0, 1, 0), Z = new Vector3f(0, 0, 1);
        quad(vc, p, cx, cy, z1, X, Y, hx, hy, new Vector3f(0, 0, 1), side, light);
        quad(vc, p, cx, cy, z0, X, Y, hx, hy, new Vector3f(0, 0, -1), side, light);
        quad(vc, p, x1, cy, cz, Z, Y, hz, hy, new Vector3f(1, 0, 0), side, light);
        quad(vc, p, x0, cy, cz, Z, Y, hz, hy, new Vector3f(-1, 0, 0), side, light);
        quad(vc, p, cx, y1, cz, X, Z, hx, hz, new Vector3f(0, 1, 0), top, light);
        quad(vc, p, cx, y0, cz, X, Z, hx, hz, new Vector3f(0, -1, 0), top, light);
    }

    // =================================================================== props
    private static void props(GarageControllerBlockEntity garage, BlockRenderDispatcher blocks, float time, float s,
                              PoseStack pose, MultiBufferSource buffers) {
        Level level = garage.getLevel();
        BlockPos origin = garage.getBlockPos();
        BlockPos min = garage.getBayMin();
        BlockPos max = garage.getBayMax();
        float ix0 = min.getX() - origin.getX(), ix1 = max.getX() + 1 - origin.getX();
        float iz0 = min.getZ() - origin.getZ(), iz1 = max.getZ() + 1 - origin.getZ();
        float floor = min.getY() - origin.getY();
        float roof = max.getY() + 1 - origin.getY();
        int w = max.getX() - min.getX() + 1;
        int dpt = max.getZ() - min.getZ() + 1;
        int light = LevelRenderer.getLightColor(level, garage.padCenter() == null ? min : garage.padCenter().above());

        // Hoist I-beam across the roof, above the lift.
        Direction in = garage.outside().getOpposite();
        float pcx = (ix0 + ix1) / 2, pcz = (iz0 + iz1) / 2;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        PoseStack.Pose p = pose.last();
        if (in.getAxis() == Direction.Axis.X) {
            box(vc, p, ix0, roof - 0.28f * s, pcz - 0.16f, ix1, roof, pcz + 0.16f, SOOT, RUST, light);
        } else {
            box(vc, p, pcx - 0.16f, roof - 0.28f * s, iz0, pcx + 0.16f, roof, iz1, SOOT, RUST, light);
        }

        // Corner props, nearest corners to the controller first.
        if (w >= 5 && dpt >= 5) {
            List<float[]> corners = new ArrayList<>(List.of(new float[]{ix0 + 0.5f, iz0 + 0.5f},
                    new float[]{ix1 - 0.5f, iz0 + 0.5f}, new float[]{ix0 + 0.5f, iz1 - 0.5f}, new float[]{ix1 - 0.5f, iz1 - 0.5f}));
            corners.sort(Comparator.comparingDouble(cn -> (cn[0] - 0.5) * (cn[0] - 0.5) + (cn[1] - 0.5) * (cn[1] - 0.5)));
            workbench(blocks, pose, buffers, corners.get(0)[0], floor, corners.get(0)[1], s, light);
            tyres(pose, buffers, corners.get(1)[0], floor, corners.get(1)[1], s, light);
            drums(pose, buffers, corners.get(2)[0], floor, corners.get(2)[1], s, light);
            cans(pose, buffers, corners.get(3)[0], floor, corners.get(3)[1], s, light);
        }

        // Spikes along the roof edge.
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int top = max.getY() + 2;
        BlockState spike = Blocks.POINTED_DRIPSTONE.defaultBlockState();
        for (int x = min.getX() - 1; x <= max.getX() + 1; x++) {
            for (int z = min.getZ() - 1; z <= max.getZ() + 1; z++) {
                boolean edge = x == min.getX() - 1 || x == max.getX() + 1 || z == min.getZ() - 1 || z == max.getZ() + 1;
                if (!edge || ((x + z) & 1) != 0) {
                    continue;
                }
                m.set(x, top, z);
                if (!level.getBlockState(m).isAir() || !level.getBlockState(m.below()).isSolidRender(level, m.below())) {
                    continue;
                }
                pose.pushPose();
                pose.translate(x - origin.getX() + 0.5, top - origin.getY(), z - origin.getZ() + 0.5);
                pose.scale(0.8f * s, 0.9f * s, 0.8f * s);
                pose.translate(-0.5, 0, -0.5);
                blocks.renderSingleBlock(spike, pose, buffers, LevelRenderer.getLightColor(level, m), OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }

        // Exhaust stack on the far roof corner (its smoke comes from the controller's client tick).
        BlockPos stack = garage.exhaustBase();
        if (stack != null && level.getBlockState(stack).isAir()) {
            float sx = stack.getX() - origin.getX() + 0.5f, sy = stack.getY() - origin.getY(), sz = stack.getZ() - origin.getZ() + 0.5f;
            float hgt = (float) GarageControllerBlockEntity.EXHAUST_HEIGHT * s;
            int sl = LevelRenderer.getLightColor(level, stack);
            vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
            p = pose.last();
            box(vc, p, sx - 0.3f, sy, sz - 0.3f, sx + 0.3f, sy + 0.3f, sz + 0.3f, RUST, RUST, sl);
            box(vc, p, sx - 0.17f, sy, sz - 0.17f, sx + 0.17f, sy + hgt, sz + 0.17f, SOOT, SOOT, sl);
            box(vc, p, sx - 0.22f, sy + hgt - 0.12f, sz - 0.22f, sx + 0.22f, sy + hgt, sz + 0.22f, RUST, SOOT, sl);
        }

        // Loose chains swaying from the roof beside the walls.
        float[][] loose = {{ix0 + 0.4f, pcz, 1.3f}, {ix1 - 0.4f, pcz + 0.6f, 1.7f}, {pcx - 0.7f, iz0 + 0.4f, 1.1f}};
        for (int k = 0; k < loose.length; k++) {
            pose.pushPose();
            pose.translate(loose[k][0], roof, loose[k][1]);
            pose.mulPose(Axis.ZP.rotationDegrees(4f * Mth.sin(time * 0.06f + k * 2.1f)));
            pose.mulPose(Axis.XP.rotationDegrees(3f * Mth.cos(time * 0.05f + k)));
            float len = loose[k][2] * s;
            float y = 0;
            while (y < len - 0.01f) {
                float seg = Math.min(1f, len - y);
                pose.pushPose();
                pose.translate(-0.5, -y - seg, -0.5);
                pose.scale(1f, seg, 1f);
                blocks.renderSingleBlock(Blocks.CHAIN.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
                y += seg;
            }
            pose.popPose();
        }
    }

    private static void workbench(BlockRenderDispatcher blocks, PoseStack pose, MultiBufferSource buffers,
                                  float cx, float floor, float cz, float s, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        PoseStack.Pose p = pose.last();
        float h = 0.82f * s;
        box(vc, p, cx - 0.46f, floor + h, cz - 0.46f, cx + 0.46f, floor + h + 0.1f, cz + 0.46f, BENCH, BENCH, light);
        for (float[] l : new float[][]{{-0.4f, -0.4f}, {0.32f, -0.4f}, {-0.4f, 0.32f}, {0.32f, 0.32f}}) {
            box(vc, p, cx + l[0], floor, cz + l[1], cx + l[0] + 0.08f, floor + h, cz + l[1] + 0.08f, SOOT, SOOT, light);
        }
        box(vc, p, cx - 0.42f, floor + 0.25f, cz - 0.42f, cx + 0.4f, floor + 0.3f, cz + 0.4f, BENCH, BENCH, light);
        // A jerry can and a drum on the shelf, a vice and a torch on top.
        box(vc, p, cx - 0.3f, floor + 0.3f, cz - 0.1f, cx - 0.12f, floor + 0.62f, cz + 0.2f, JERRY, JERRY, light);
        float top = floor + h + 0.1f;
        pose.pushPose();
        pose.translate(cx - 0.35, top, cz - 0.3);
        pose.scale(0.32f * s, 0.32f * s, 0.32f * s);
        blocks.renderSingleBlock(Blocks.ANVIL.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.pushPose();
        pose.translate(cx + 0.05, top, cz + 0.05);
        pose.scale(0.6f * s, 0.6f * s, 0.6f * s);
        blocks.renderSingleBlock(Blocks.REDSTONE_TORCH.defaultBlockState(), pose, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.pushPose();
        pose.translate(cx + 0.12, top, cz - 0.4);
        pose.scale(0.3f * s, 0.3f * s, 0.3f * s);
        blocks.renderSingleBlock(Blocks.LANTERN.defaultBlockState(), pose, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static void tyres(PoseStack pose, MultiBufferSource buffers, float cx, float floor, float cz, float s, int light) {
        for (int k = 0; k < 3; k++) {
            pose.pushPose();
            pose.translate(cx, floor + k * 0.27f * s, cz);
            pose.mulPose(Axis.YP.rotationDegrees(k * 17f));
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
            box(vc, pose.last(), -0.4f, 0, -0.4f, 0.4f, 0.26f * s, 0.4f, TYRE, TYRE_TOP, light);
            pose.popPose();
        }
    }

    private static void drums(PoseStack pose, MultiBufferSource buffers, float cx, float floor, float cz, float s, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        PoseStack.Pose p = pose.last();
        box(vc, p, cx - 0.3f, floor, cz - 0.3f, cx + 0.25f, floor + 0.85f * s, cz + 0.25f, DRUM, DRUM_TOP, light);
        pose.pushPose();
        pose.translate(cx + 0.35, floor, cz + 0.2);
        pose.mulPose(Axis.YP.rotationDegrees(25));
        box(buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), pose.last(), -0.22f, 0, -0.22f, 0.22f, 0.7f * s, 0.22f,
                DRUM, DRUM_TOP, light);
        pose.popPose();
    }

    private static void cans(PoseStack pose, MultiBufferSource buffers, float cx, float floor, float cz, float s, int light) {
        for (int k = 0; k < 3; k++) {
            pose.pushPose();
            pose.translate(cx - 0.25 + k * 0.24, floor, cz + (k == 1 ? 0.15 : -0.1));
            pose.mulPose(Axis.YP.rotationDegrees(k * 31f - 20f));
            box(buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), pose.last(), -0.09f, 0, -0.16f, 0.09f, 0.42f * s, 0.16f,
                    JERRY, JERRY, light);
            pose.popPose();
        }
        // A spare tyre lying beside them.
        pose.pushPose();
        pose.translate(cx + 0.1, floor, cz + 0.3);
        box(buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), pose.last(), -0.35f, 0, -0.35f, 0.35f, 0.22f * s, 0.35f,
                TYRE, TYRE_TOP, light);
        pose.popPose();
    }
}
