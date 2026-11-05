package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.block.GaragePlan;
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
 * The see-through Garage plan in the world: every block still to place (walls, floor, ceiling, emitters), anything in
 * the way, and a checklist on screen. Placed controllers keep showing their plan (at the size picked in the Garage
 * Planner) until it's turned off there or the Garage is finished. Holding a controller previews where one would go;
 * sneak + scroll changes that preview's size, and the controller takes that size when placed.
 */
@EventBusSubscriber(modid = GolemGarage.MOD_ID, value = Dist.CLIENT)
public final class GaragePlanOverlay {
    /** Preview sizes for a controller in hand: width, depth, height. */
    private static final int[][] SIZES = {{3, 3, 4}, {5, 5, 4}, {7, 7, 5}, {9, 9, 6}};
    private static int size = 1;

    private static final List<GaragePlan.Cell> cells = new ArrayList<>();
    /** The plan the checklist describes: the preview, or the nearest placed plan. */
    private static final List<GaragePlan.Cell> focus = new ArrayList<>();
    private static final Map<GaragePlan.Kind, int[]> counts = new EnumMap<>(GaragePlan.Kind.class);
    private static boolean active;
    private static boolean holdingNow;
    private static boolean placed;
    private static boolean complete;
    private static AABB readyBox;
    private static int[] dims = SIZES[1];
    /** Where the preview last sat, so a controller placed there can take the preview's size. */
    private static BlockPos previewAt;
    private static int previewAge;

    private static boolean holding(LocalPlayer p) {
        return p.getMainHandItem().is(ModItems.GARAGE_CONTROLLER.get())
                || p.getOffhandItem().is(ModItems.GARAGE_CONTROLLER.get());
    }

    // ================================================================ planning
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        cells.clear();
        focus.clear();
        counts.clear();
        readyBox = null;
        complete = false;
        placed = false;
        active = false;
        if (player == null || mc.level == null || !com.xtrip.golemgarage.Config.SHOW_GARAGE_PLAN.get()) {
            return;
        }
        Level level = mc.level;
        holdingNow = holding(player);

        // A controller just placed where the preview was takes the preview's size.
        if (previewAt != null && ++previewAge < 60 && level.getBlockEntity(previewAt) instanceof GarageControllerBlockEntity fresh
                && !fresh.configured()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.xtrip.golemgarage.network.GaragePlanPayload(
                    previewAt, com.xtrip.golemgarage.network.GaragePlanPayload.SET, dims[0], dims[1], dims[2], true));
            previewAt = null;
        }

        // Every placed controller nearby with its plan switched on.
        GarageControllerBlockEntity nearest = null;
        double nearestD = Double.MAX_VALUE;
        for (GarageControllerBlockEntity g : controllers(level, player.blockPosition(), 3)) {
            double dist = g.getBlockPos().distSqr(player.blockPosition());
            if (g.isFormed()) {
                if (holdingNow && dist < 16 * 16 && g.getBayMin() != null && g.getBayMax() != null) {
                    complete = true;
                    readyBox = new AABB(Vec3.atLowerCornerOf(g.getBayMin()), Vec3.atLowerCornerOf(g.getBayMax().offset(1, 1, 1)));
                }
                continue;
            }
            if (!g.showPlan() && !(holdingNow && dist < 10 * 10)) {
                continue;
            }
            if (dist > 48 * 48) {
                continue;
            }
            List<GaragePlan.Cell> plan = g.plan();
            cells.addAll(plan);
            if (dist < nearestD && dist < 16 * 16) {
                nearestD = dist;
                nearest = g;
                focus.clear();
                focus.addAll(plan);
                dims = new int[]{g.planW(), g.planD(), g.planH()};
            }
        }
        placed = nearest != null;

        // Holding a controller and not near a placed one: preview where it would go.
        if (holdingNow && nearest == null && mc.hitResult instanceof BlockHitResult hit
                && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            dims = SIZES[size];
            BlockPos target = hit.getBlockPos();
            BlockState s = level.getBlockState(target);
            BlockPos c = s.canBeReplaced() ? target : target.relative(hit.getDirection());
            List<GaragePlan.Cell> plan = GaragePlan.plan(level, c, player.getDirection(), dims[0], dims[1], dims[2]);
            cells.addAll(plan);
            focus.addAll(plan);
            previewAt = c.immutable();
            previewAge = 0;
        }
        for (GaragePlan.Cell c : focus) {
            int[] n = counts.computeIfAbsent(c.kind(), k -> new int[2]);
            n[0]++;
            if (c.done()) {
                n[1]++;
            }
        }
        active = !cells.isEmpty() || holdingNow || readyBox != null;
    }

    private static List<GarageControllerBlockEntity> controllers(Level level, BlockPos center, int chunkRadius) {
        List<GarageControllerBlockEntity> out = new ArrayList<>();
        ChunkPos cp = new ChunkPos(center);
        for (int cx = cp.x - chunkRadius; cx <= cp.x + chunkRadius; cx++) {
            for (int cz = cp.z - chunkRadius; cz <= cp.z + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof GarageControllerBlockEntity g) {
                        out.add(g);
                    }
                }
            }
        }
        return out;
    }

    // ================================================================ input
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.player.isShiftKeyDown() || !holding(mc.player) || placed) {
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
        for (GaragePlan.Cell c : cells) {
            if (c.done()) {
                continue;
            }
            int col = c.wrong() ? 0xFF4A4A : c.kind().color;
            float a = (c.wrong() ? 0.30f : 0.16f) * pulse;
            AABB box = new AABB(c.pos()).deflate(0.06);
            LevelRenderer.addChainedFilledBoxVertices(pose, fill, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                    r(col), g(col), b(col), a);
        }
        buffers.endBatch(RenderType.debugFilledBox());

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (GaragePlan.Cell c : cells) {
            if (c.done()) {
                continue;
            }
            int col = c.wrong() ? 0xFF4A4A : c.kind().color;
            LevelRenderer.renderLineBox(pose, lines, new AABB(c.pos()).deflate(0.04), r(col), g(col), b(col), 0.9f);
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
        if (!active || mc.options.hideGui || mc.screen != null || focus.isEmpty() && !holdingNow) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        List<String[]> rows = new ArrayList<>();
        String title;
        int titleColor = 0xFFFFD84A;
        if (complete && focus.isEmpty()) {
            title = I18n.get("golemgarage.plan.ready");
            titleColor = 0xFF5AFF8A;
            for (var l : font.split(Component.translatable("golemgarage.plan.ready_help"), 184)) {
                rows.add(new String[]{text(l), ""});
            }
        } else if (focus.isEmpty()) {
            title = I18n.get("golemgarage.plan.title");
            for (var l : font.split(Component.translatable("golemgarage.plan.look"), 184)) {
                rows.add(new String[]{text(l), ""});
            }
        } else {
            title = I18n.get(placed ? "golemgarage.plan.finish" : "golemgarage.plan.title_size",
                    dims[0], dims[1], dims[2]);
            boolean allDone = true;
            for (GaragePlan.Kind k : GaragePlan.Kind.values()) {
                int[] n = counts.get(k);
                if (n == null) {
                    continue;
                }
                int left = n[0] - n[1];
                if (k == GaragePlan.Kind.CLEAR) {
                    rows.add(new String[]{k.name(), I18n.get("golemgarage.plan.in_the_way", left), "bad"});
                    allDone = false;
                    continue;
                }
                if (k == GaragePlan.Kind.CONTROLLER && !placed) {
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
        int hBox = 18 + rows.size() * 10 + (focus.isEmpty() ? 0 : 12);
        g.fill(x - 3, y - 3, x + wBox, y + hBox, 0xA0101420);
        g.renderOutline(x - 3, y - 3, wBox + 3, hBox + 3, 0xFF3A4458);
        g.drawString(font, font.plainSubstrByWidth(title, wBox - 4), x, y, titleColor);
        y += 13;
        for (String[] r : rows) {
            if (r.length == 2) {
                g.drawString(font, r[0], x, y, 0xFFC8D0DC);
            } else {
                GaragePlan.Kind k = GaragePlan.Kind.valueOf(r[0]);
                g.fill(x, y + 1, x + 6, y + 7, 0xFF000000 | k.color);
                g.drawString(font, I18n.get(k.key()), x + 10, y, 0xFFE8ECF2);
                int vc = "ok".equals(r[2]) ? 0xFF5AFF8A : "bad".equals(r[2]) ? 0xFFFF6A6A : 0xFFFFE27A;
                g.drawString(font, r[1], x + wBox - 6 - font.width(r[1]), y, vc);
            }
            y += 10;
        }
        if (!focus.isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth(I18n.get(placed ? "golemgarage.plan.open_planner"
                    : "golemgarage.plan.scroll"), wBox - 4), x, y + 2, 0xFF7D8696);
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
