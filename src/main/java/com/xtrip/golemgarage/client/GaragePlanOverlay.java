package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.block.GarageBay;
import com.xtrip.golemgarage.block.GarageControllerBlock;
import com.xtrip.golemgarage.block.ModBlocks;
import com.xtrip.golemgarage.block.entity.GarageControllerBlockEntity;
import com.xtrip.golemgarage.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * While the player holds a Garage Controller, shows a see-through plan of a Garage: every block still
 * to place (walls, floor, ceiling, emitters), anything in the way, and a checklist on screen.
 * Before placing it previews where the controller would go; after, it follows the placed controller.
 * Sneak + scroll changes the room size.
 */
@EventBusSubscriber(modid = GolemGarage.MOD_ID, value = Dist.CLIENT)
public final class GaragePlanOverlay {
    /** Interior sizes: width, depth, height. */
    private static final int[][] SIZES = {{3, 3, 4}, {5, 5, 4}, {7, 7, 5}, {9, 9, 6}};
    private static int size = 1;

    enum Kind {
        WALL(0xE8E8F0), FLOOR(0x8FA4C8), CEILING(0x8FA4C8),
        EMITTER(0x5FE8FF), CONTROLLER(0xFFD84A), CLEAR(0xFF4A4A);
        final int color;

        Kind(int color) {
            this.color = color;
        }

        String key() {
            return "golemgarage.plan.kind." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    record Cell(BlockPos pos, Kind kind, boolean done, boolean wrong) {
    }

    private static final List<Cell> cells = new ArrayList<>();
    private static final Map<Kind, int[]> counts = new EnumMap<>(Kind.class);
    private static boolean active;
    private static boolean placed;
    private static boolean complete;
    private static AABB readyBox;
    private static int[] dims = SIZES[1];

    private static boolean holding(LocalPlayer p) {
        return p.getMainHandItem().is(ModItems.GARAGE_CONTROLLER.get())
                || p.getOffhandItem().is(ModItems.GARAGE_CONTROLLER.get());
    }

    // ================================================================ planning
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        active = player != null && mc.level != null && holding(player)
                && com.xtrip.golemgarage.Config.SHOW_GARAGE_PLAN.get();
        if (!active) {
            cells.clear();
            return;
        }
        dims = SIZES[size];
        Level level = mc.level;
        BlockPos controller = null;
        Direction facing = null;

        // A placed controller nearby that isn't a working Garage yet takes priority.
        BlockPos near = nearestController(level, player.blockPosition(), 10);
        readyBox = null;
        complete = false;
        if (near != null) {
            GarageBay bay = GarageBay.find(level, near);
            if (bay != null) {
                complete = true;
                readyBox = new AABB(Vec3.atLowerCornerOf(bay.min()), Vec3.atLowerCornerOf(bay.max().offset(1, 1, 1)));
            } else {
                controller = near;
                facing = level.getBlockState(near).getValue(GarageControllerBlock.FACING);
            }
        }
        placed = controller != null;
        if (controller == null && !complete && mc.hitResult instanceof BlockHitResult hit
                && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos target = hit.getBlockPos();
            BlockState s = level.getBlockState(target);
            controller = s.canBeReplaced() ? target : target.relative(hit.getDirection());
            facing = player.getDirection();
        }
        cells.clear();
        counts.clear();
        if (controller != null) {
            plan(level, controller, facing);
        }
    }

    private static BlockPos nearestController(Level level, BlockPos center, int radius) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        ChunkPos cp = new ChunkPos(center);
        for (int cx = cp.x - 1; cx <= cp.x + 1; cx++) {
            for (int cz = cp.z - 1; cz <= cp.z + 1; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof GarageControllerBlockEntity) {
                        double d = be.getBlockPos().distSqr(center);
                        if (d <= radius * radius && d < bestD) {
                            bestD = d;
                            best = be.getBlockPos();
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Lays out the room behind the controller and checks every block against the world. */
    private static void plan(Level level, BlockPos c, Direction f) {
        int w = dims[0];
        int depth = dims[1];
        int h = dims[2];
        int half = w / 2;
        Direction right = f.getClockWise();
        // Emitter spots in the ceiling.
        List<int[]> emitters = new ArrayList<>();
        int dA = 1 + (depth - 1) / 4;
        int dB = depth - (depth - 1) / 4;
        int lA = -(w / 4);
        int lB = w / 4;
        emitters.add(new int[]{dA, lA});
        emitters.add(new int[]{dB, lB});
        if (w >= 7) {
            emitters.add(new int[]{dA, lB});
            emitters.add(new int[]{dB, lA});
        }
        for (int d = 0; d <= depth + 1; d++) {
            for (int l = -half - 1; l <= half + 1; l++) {
                boolean inside = d >= 1 && d <= depth && l >= -half && l <= half;
                boolean sideWall = (d == 0 || d == depth + 1) && l >= -half && l <= half
                        || (l == -half - 1 || l == half + 1) && d >= 1 && d <= depth;
                for (int y = -1; y <= h; y++) {
                    BlockPos p = c.relative(f, d).relative(right, l).above(y);
                    BlockState s = level.getBlockState(p);
                    if (inside) {
                        if (y == -1) {
                            add(p, Kind.FLOOR, s.isSolidRender(level, p), !s.isAir() && !s.isSolidRender(level, p));
                        } else if (y == h) {
                            final int dd = d;
                            final int ll = l;
                            boolean isEmitterSpot = emitters.stream().anyMatch(e -> e[0] == dd && e[1] == ll);
                            boolean isEmitter = s.is(ModBlocks.LASER_EMITTER.get()) || s.is(ModBlocks.STEEL_EMITTER.get());
                            if (isEmitterSpot) {
                                add(p, Kind.EMITTER, isEmitter, !s.isAir() && !isEmitter);
                            } else {
                                add(p, Kind.CEILING, s.isSolidRender(level, p) || isEmitter,
                                        !s.isAir() && !s.isSolidRender(level, p) && !isEmitter);
                            }
                        } else {
                            boolean air = s.isAir();
                            if (!air) {
                                add(p, Kind.CLEAR, false, true);
                            }
                        }
                    } else if (sideWall && y >= 0 && y < h) {
                        if (d == 0 && l == 0 && y == 0) {
                            add(p, Kind.CONTROLLER, s.is(ModBlocks.GARAGE_CONTROLLER.get()), false);
                        } else {
                            add(p, Kind.WALL, s.is(GarageBay.WALLS), !s.isAir() && !s.canBeReplaced() && !s.is(GarageBay.WALLS));
                        }
                    }
                }
            }
        }
    }

    private static void add(BlockPos p, Kind kind, boolean done, boolean wrong) {
        cells.add(new Cell(p.immutable(), kind, done, wrong));
        int[] n = counts.computeIfAbsent(kind, k -> new int[2]);
        n[0]++;
        if (done) {
            n[1]++;
        }
    }

    // ================================================================ input
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.player.isShiftKeyDown() || !holding(mc.player)) {
            return;
        }
        size = Math.floorMod(size + (event.getScrollDeltaY() > 0 ? 1 : -1), SIZES.length);
        event.setCanceled(true);
    }

    // ================================================================ world rendering
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!active || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float pulse = 0.75f + 0.25f * (float) Math.sin(System.currentTimeMillis() / 300.0);

        VertexConsumer fill = buffers.getBuffer(RenderType.debugFilledBox());
        for (Cell c : cells) {
            if (c.done) {
                continue;
            }
            int col = c.wrong ? 0xFF4A4A : c.kind.color;
            float a = (c.wrong ? 0.30f : 0.16f) * pulse;
            AABB box = new AABB(c.pos).deflate(0.06);
            LevelRenderer.addChainedFilledBoxVertices(pose, fill, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                    r(col), g(col), b(col), a);
        }
        buffers.endBatch(RenderType.debugFilledBox());

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Cell c : cells) {
            if (c.done) {
                continue;
            }
            int col = c.wrong ? 0xFF4A4A : c.kind.color;
            LevelRenderer.renderLineBox(pose, lines, new AABB(c.pos).deflate(0.04), r(col), g(col), b(col), 0.9f);
        }
        if (readyBox != null) {
            LevelRenderer.renderLineBox(pose, lines, readyBox.inflate(0.02), 0.35f, 1f, 0.5f, 0.9f);
        }
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    private static float r(int c) {
        return ((c >> 16) & 0xFF) / 255f;
    }

    private static float g(int c) {
        return ((c >> 8) & 0xFF) / 255f;
    }

    private static float b(int c) {
        return (c & 0xFF) / 255f;
    }

    // ================================================================ checklist
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.options.hideGui || mc.screen != null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        List<String[]> rows = new ArrayList<>();
        String title;
        int titleColor = 0xFFFFD84A;
        if (complete && cells.isEmpty()) {
            title = I18n.get("golemgarage.plan.ready");
            titleColor = 0xFF5AFF8A;
            for (var l : font.split(Component.translatable("golemgarage.plan.ready_help"), 184)) {
                rows.add(new String[]{text(l), ""});
            }
        } else if (cells.isEmpty()) {
            title = I18n.get("golemgarage.plan.title");
            for (var l : font.split(Component.translatable("golemgarage.plan.look"), 184)) {
                rows.add(new String[]{text(l), ""});
            }
        } else {
            title = I18n.get(placed ? "golemgarage.plan.finish" : "golemgarage.plan.title_size",
                    dims[0], dims[1], dims[2]);
            boolean allDone = true;
            for (Kind k : Kind.values()) {
                int[] n = counts.get(k);
                if (n == null) {
                    continue;
                }
                int left = n[0] - n[1];
                if (k == Kind.CLEAR) {
                    rows.add(new String[]{k.name(), I18n.get("golemgarage.plan.in_the_way", left), "bad"});
                    allDone = false;
                    continue;
                }
                if (k == Kind.CONTROLLER && !placed) {
                    rows.add(new String[]{k.name(), I18n.get("golemgarage.plan.place_here"), ""});
                    allDone = false;
                    continue;
                }
                rows.add(new String[]{k.name(), left == 0 ? I18n.get("golemgarage.plan.done")
                        : I18n.get("golemgarage.plan.to_place", left), left == 0 ? "ok" : ""});
                if (left > 0) {
                    allDone = false;
                }
            }
            if (allDone) {
                for (var l : font.split(Component.translatable("golemgarage.plan.all_placed"), 184)) {
                    rows.add(new String[]{text(l), ""});
                }
            }
        }
        int x = 6;
        int y = 6;
        int wBox = 190;
        int hBox = 18 + rows.size() * 10 + (cells.isEmpty() ? 0 : 12);
        g.fill(x - 3, y - 3, x + wBox, y + hBox, 0xA0101420);
        g.renderOutline(x - 3, y - 3, wBox + 3, hBox + 3, 0xFF3A4458);
        g.drawString(font, font.plainSubstrByWidth(title, wBox - 4), x, y, titleColor);
        y += 13;
        for (String[] r : rows) {
            if (r.length == 2) {
                g.drawString(font, r[0], x, y, 0xFFC8D0DC);
            } else {
                Kind k = Kind.valueOf(r[0]);
                g.fill(x, y + 1, x + 6, y + 7, 0xFF000000 | k.color);
                g.drawString(font, I18n.get(k.key()), x + 10, y, 0xFFE8ECF2);
                int vc = "ok".equals(r[2]) ? 0xFF5AFF8A : "bad".equals(r[2]) ? 0xFFFF6A6A : 0xFFFFE27A;
                g.drawString(font, r[1], x + wBox - 6 - font.width(r[1]), y, vc);
            }
            y += 10;
        }
        if (!cells.isEmpty()) {
            g.drawString(font, Component.translatable("golemgarage.plan.scroll"), x, y + 2, 0xFF7D8696);
        }
    }

    private static String text(net.minecraft.util.FormattedCharSequence seq) {
        StringBuilder b = new StringBuilder();
        seq.accept((i, style, cp) -> {
            b.appendCodePoint(cp);
            return true;
        });
        return b.toString();
    }

    private GaragePlanOverlay() {
    }
}
