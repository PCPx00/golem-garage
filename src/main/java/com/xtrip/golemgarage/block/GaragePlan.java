package com.xtrip.golemgarage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The layout of a Garage of a chosen size behind its controller, and what it takes to build it. Shared by the
 * on-screen plan, the Garage Planner screen and the controller's auto-build, so all three agree.
 */
public final class GaragePlan {
    public static final int MIN_W = 3, MAX_W = 9, MIN_D = 3, MAX_D = 9, MIN_H = 4, MAX_H = 8;

    public enum Kind {
        WALL(0xE8E8F0), FLOOR(0x8FA4C8), CEILING(0x8FA4C8),
        EMITTER(0x5FE8FF), CONTROLLER(0xFFD84A), DOOR(0xFF9A3C), WINDOW(0xB8F0FF), CLEAR(0xFF4A4A);
        public final int color;

        Kind(int color) {
            this.color = color;
        }

        public String key() {
            return "golemgarage.plan.kind." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** One block of the plan: what goes there, whether it's already right, and whether something else is in the way. */
    public record Cell(BlockPos pos, Kind kind, boolean done, boolean wrong) {
    }

    private GaragePlan() {
    }

    /** Widths are odd so the room sits centred on the controller. */
    public static int clampWidth(int w) {
        w = Math.max(MIN_W, Math.min(MAX_W, w));
        return w % 2 == 0 ? w - 1 : w;
    }

    public static int clampDepth(int d) {
        return Math.max(MIN_D, Math.min(MAX_D, d));
    }

    public static int clampHeight(int h) {
        return Math.max(MIN_H, Math.min(MAX_H, h));
    }

    /** Ceiling emitter spots as (depth, sideways) pairs: 2 in small rooms, 3 from 5 wide, 4 from 7 wide. */
    public static List<int[]> emitterSpots(int w, int depth) {
        List<int[]> out = new ArrayList<>();
        int dA = 1 + (depth - 1) / 4;
        int dB = depth - (depth - 1) / 4;
        int lA = -(w / 4);
        int lB = w / 4;
        out.add(new int[]{dA, lA});
        out.add(new int[]{dB, lB});
        if (w >= 7) {
            out.add(new int[]{dA, lB});
            out.add(new int[]{dB, lA});
        } else if (w >= 5) {
            out.add(new int[]{(1 + depth + 1) / 2, 0});
        }
        return out;
    }

    /** Lays out the room behind the controller at c, facing f, and checks every block against the world. */
    public static List<Cell> plan(Level level, BlockPos c, Direction f, int w, int depth, int h) {
        return plan(level, c, f, w, depth, h, GarageStyle.WASTELAND);
    }

    public static List<Cell> plan(Level level, BlockPos c, Direction f, int w, int depth, int h, GarageStyle style) {
        List<Cell> cells = new ArrayList<>();
        int half = w / 2;
        Direction right = f.getClockWise();
        List<int[]> emitters = emitterSpots(w, depth);
        for (int d = 0; d <= depth + 1; d++) {
            for (int l = -half - 1; l <= half + 1; l++) {
                boolean inside = d >= 1 && d <= depth && l >= -half && l <= half;
                boolean sideWall = (d == 0 || d == depth + 1) && l >= -half && l <= half
                        || (l == -half - 1 || l == half + 1) && d >= 1 && d <= depth;
                for (int y = -1; y <= h; y++) {
                    BlockPos p = c.relative(f, d).relative(right, l).above(y);
                    BlockState s = level.getBlockState(p);
                    boolean free = s.isAir() || s.canBeReplaced();
                    if (inside) {
                        if (y == -1) {
                            boolean solid = s.isSolidRender(level, p);
                            cells.add(new Cell(p.immutable(), Kind.FLOOR, solid, !solid && !free));
                        } else if (y == h) {
                            final int dd = d;
                            final int ll = l;
                            boolean spot = emitters.stream().anyMatch(e -> e[0] == dd && e[1] == ll);
                            boolean emitter = isEmitter(s);
                            if (spot) {
                                cells.add(new Cell(p.immutable(), Kind.EMITTER, emitter, !emitter && !free));
                            } else if (style.skylight()) {
                                boolean ok = s.isSolidRender(level, p) || glass(level, p, s) || emitter;
                                cells.add(new Cell(p.immutable(), Kind.WINDOW, ok, !ok && !free));
                            } else {
                                boolean ok = s.isSolidRender(level, p) || emitter;
                                cells.add(new Cell(p.immutable(), Kind.CEILING, ok, !ok && !free));
                            }
                        } else if (!GarageBay.open(s)) {
                            // Grass and flowers can just be cleared; anything else has to be moved first.
                            cells.add(new Cell(p.immutable(), Kind.CLEAR, false, !s.canBeReplaced()));
                        }
                    } else if (sideWall && y >= 0 && y < h) {
                        boolean wall = s.is(GarageBay.WALLS);
                        if (d == 0 && l == 0 && y == 0) {
                            cells.add(new Cell(p.immutable(), Kind.CONTROLLER, s.is(ModBlocks.GARAGE_CONTROLLER.get()), false));
                        } else if (d == depth + 1 && Math.abs(l) <= (w >= 5 ? 1 : 0) && y <= Math.min(2, h - 2)) {
                            cells.add(new Cell(p.immutable(), Kind.DOOR, wall, !wall && !free));
                        } else if (style.window(d, l, y, w, depth, h)) {
                            // A solid wall block already there is fine too.
                            cells.add(new Cell(p.immutable(), Kind.WINDOW, wall, !wall && !free));
                        } else {
                            cells.add(new Cell(p.immutable(), Kind.WALL, wall, !wall && !free));
                        }
                    }
                }
            }
        }
        return cells;
    }

    /** See-through blocks that count as Garage wall or roof: glass, panes, Garage Windows. */
    public static boolean glass(Level level, BlockPos p, BlockState s) {
        return s.is(GarageBay.WALLS) && !s.isSolidRender(level, p);
    }

    private static boolean isEmitter(BlockState s) {
        return s.is(ModBlocks.LASER_EMITTER.get()) || s.is(ModBlocks.STEEL_EMITTER.get());
    }

    // =================================================================== materials
    /** What still has to be placed, what's blocking, and what the player has to hand. */
    public record Needs(int walls, int windows, int solid, int emitters, int blocked, int haveWalls, int haveWindows,
                        int haveSolid, int haveEmitters, boolean canBuild) {
        public int total() {
            return walls + windows + solid + emitters;
        }
    }

    public static boolean windowBlock(Item item, Level level) {
        return item instanceof BlockItem bi && !(bi.getBlock() instanceof EntityBlock)
                && bi.getBlock().defaultBlockState().is(GarageBay.WALLS)
                && !bi.getBlock().defaultBlockState().isSolidRender(level, BlockPos.ZERO);
    }

    public static boolean wallBlock(Item item) {
        return item instanceof BlockItem bi && !(bi.getBlock() instanceof EntityBlock)
                && bi.getBlock().defaultBlockState().is(GarageBay.WALLS);
    }

    public static boolean solidBlock(Item item, Level level) {
        if (!(item instanceof BlockItem bi) || bi.getBlock() instanceof EntityBlock) {
            return false;
        }
        BlockState s = bi.getBlock().defaultBlockState();
        return s.isSolidRender(level, BlockPos.ZERO) && !s.hasBlockEntity() && !(s.getBlock() instanceof net.minecraft.world.level.block.FallingBlock);
    }

    public static boolean emitterItem(Item item) {
        return item instanceof BlockItem bi && (bi.getBlock() == ModBlocks.LASER_EMITTER.get()
                || bi.getBlock() == ModBlocks.STEEL_EMITTER.get());
    }

    /** The inventory slots a build may draw from: the main inventory and the off hand. */
    private static List<ItemStack> stacks(Player player) {
        List<ItemStack> out = new ArrayList<>(player.getInventory().items);
        out.addAll(player.getInventory().offhand);
        return out;
    }

    public static Needs needs(List<Cell> cells, Player player) {
        int walls = 0, windows = 0, solid = 0, emitters = 0, blocked = 0;
        for (Cell c : cells) {
            if (c.wrong()) {
                blocked++;
            }
            if (c.done()) {
                continue;
            }
            switch (c.kind()) {
                case WALL, DOOR -> walls++;
                case FLOOR, CEILING -> solid++;
                case WINDOW -> windows++;
                case EMITTER -> emitters++;
                default -> {
                }
            }
        }
        int hw = 0, hg = 0, hs = 0, he = 0;
        for (ItemStack s : stacks(player)) {
            if (windowBlock(s.getItem(), player.level())) {
                hg += s.getCount();
            } else if (wallBlock(s.getItem())) {
                hw += s.getCount();
            }
            if (solidBlock(s.getItem(), player.level())) {
                hs += s.getCount();
            }
            if (emitterItem(s.getItem())) {
                he += s.getCount();
            }
        }
        boolean creative = player.getAbilities().instabuild;
        boolean placed = cells.stream().anyMatch(c -> c.kind() == Kind.CONTROLLER && c.done());
        boolean enough = creative || allocate(player, walls, windows, solid, emitters, true) != null;
        return new Needs(walls, windows, solid, emitters, blocked, hw, hg, hs, he, placed && blocked == 0 && enough
                && walls + windows + solid + emitters + (int) cells.stream().filter(c -> c.kind() == Kind.CLEAR).count() > 0);
    }

    /** The blocks a build will use, in order, for walls, for floor and roof, and for emitters. */
    public record Supply(List<BlockState> walls, List<BlockState> windows, List<BlockState> solid, List<BlockState> emitters) {
    }

    /**
     * Picks blocks from the player's inventory: walls from the garage_walls blocks they carry most of, floor and roof
     * from any solid block left over, emitters steel first. Returns null if they don't have enough. Unless simulating,
     * takes the items.
     */
    @Nullable
    public static Supply allocate(Player player, int walls, int windows, int solid, int emitters, boolean simulate) {
        List<ItemStack> inv = stacks(player);
        int[] left = new int[inv.size()];
        for (int i = 0; i < inv.size(); i++) {
            left[i] = inv.get(i).getCount();
        }
        List<BlockState> g = take(inv, left, windows, s -> windowBlock(s.getItem(), player.level()));
        if (g == null) {
            return null;
        }
        // Walls: solid wall blocks first; glass only if that's all there is.
        List<BlockState> w = takeUpTo(inv, left, walls, s -> wallBlock(s.getItem()) && !windowBlock(s.getItem(), player.level()));
        if (w.size() < walls) {
            List<BlockState> more = take(inv, left, walls - w.size(), s -> wallBlock(s.getItem()));
            if (more == null) {
                return null;
            }
            w.addAll(more);
        }
        List<BlockState> f = take(inv, left, solid, s -> solidBlock(s.getItem(), player.level()));
        if (f == null) {
            return null;
        }
        // Steel emitters first, so a big room reaches the Industrial Bay tier.
        List<BlockState> e = takeUpTo(inv, left, emitters, s -> s.getItem() instanceof BlockItem bi
                && bi.getBlock() == ModBlocks.STEEL_EMITTER.get());
        List<BlockState> laser = take(inv, left, emitters - e.size(), s -> s.getItem() instanceof BlockItem bi
                && bi.getBlock() == ModBlocks.LASER_EMITTER.get());
        if (laser == null) {
            return null;
        }
        e.addAll(laser);
        if (!simulate) {
            for (int i = 0; i < inv.size(); i++) {
                int used = inv.get(i).getCount() - left[i];
                if (used > 0) {
                    inv.get(i).shrink(used);
                }
            }
            player.getInventory().setChanged();
        }
        return new Supply(w, g, f, e);
    }

    /** A creative player's build in the style's own blocks, glass windows, and steel emitters where the tier wants them. */
    public static Supply creative(GarageStyle style, int walls, int windows, int solid, int emitters) {
        List<BlockState> w = new ArrayList<>(), g = new ArrayList<>(), f = new ArrayList<>(), e = new ArrayList<>();
        for (int i = 0; i < walls; i++) {
            w.add(style.creativeWall.defaultBlockState());
        }
        for (int i = 0; i < windows; i++) {
            g.add(Blocks.GLASS.defaultBlockState());
        }
        for (int i = 0; i < solid; i++) {
            f.add(style.creativeSolid.defaultBlockState());
        }
        for (int i = 0; i < emitters; i++) {
            e.add((i % 2 == 0 ? ModBlocks.STEEL_EMITTER.get() : ModBlocks.LASER_EMITTER.get()).defaultBlockState());
        }
        return new Supply(w, g, f, e);
    }

    @Nullable
    private static List<BlockState> take(List<ItemStack> inv, int[] left, int count, java.util.function.Predicate<ItemStack> ok) {
        List<BlockState> out = takeUpTo(inv, left, count, ok);
        return out.size() < count ? null : out;
    }

    /** Takes up to count blocks, the most plentiful kind first so the walls come out in one material. */
    private static List<BlockState> takeUpTo(List<ItemStack> inv, int[] left, int count, java.util.function.Predicate<ItemStack> ok) {
        List<BlockState> out = new ArrayList<>();
        if (count <= 0) {
            return out;
        }
        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.get(i);
            if (!s.isEmpty() && left[i] > 0 && ok.test(s)) {
                totals.merge(s.getItem(), left[i], Integer::sum);
            }
        }
        List<Item> order = new ArrayList<>(totals.keySet());
        order.sort((a, b) -> totals.get(b) - totals.get(a));
        for (Item item : order) {
            Block block = ((BlockItem) item).getBlock();
            for (int i = 0; i < inv.size() && out.size() < count; i++) {
                if (inv.get(i).is(item) && left[i] > 0) {
                    int n = Math.min(left[i], count - out.size());
                    left[i] -= n;
                    for (int k = 0; k < n; k++) {
                        out.add(block.defaultBlockState());
                    }
                }
            }
            if (out.size() >= count) {
                break;
            }
        }
        return out;
    }
}
