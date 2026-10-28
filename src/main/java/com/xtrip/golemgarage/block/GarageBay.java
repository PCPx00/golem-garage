package com.xtrip.golemgarage.block;

import com.xtrip.golemgarage.GolemGarage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A valid Garage bay: an enclosed air room that the Garage Controller can repair in.
 * Rules: 3-9 wide, 3-9 deep, 4-8 tall. Side walls must be garage_walls blocks.
 * The floor and ceiling can be any solid block. 2-4 Laser Emitters sit on the ceiling.
 */
public record GarageBay(BlockPos min, BlockPos max, List<BlockPos> emitters) {
    public static final TagKey<Block> WALLS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "garage_walls"));

    private static final int MIN_XZ = 3;
    private static final int MAX_XZ = 9;
    private static final int MIN_Y = 4;
    private static final int MAX_Y = 8;
    private static final int MIN_EMITTERS = 2;
    private static final int MAX_EMITTERS = 4;
    private static final int MAX_AIR = MAX_XZ * MAX_XZ * MAX_Y;

    /** Finds a valid bay touching the controller, or null if there is none. */
    public static GarageBay find(Level level, BlockPos controller) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos start = controller.relative(d);
            if (level.getBlockState(start).isAir()) {
                GarageBay bay = flood(level, controller, start);
                if (bay != null) {
                    return bay;
                }
            }
        }
        return null;
    }

    private static GarageBay flood(Level level, BlockPos controller, BlockPos start) {
        int minX = start.getX(), maxX = start.getX();
        int minY = start.getY(), maxY = start.getY();
        int minZ = start.getZ(), maxZ = start.getZ();

        Set<BlockPos> air = new HashSet<>();
        Set<BlockPos> emitters = new LinkedHashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        air.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
            if (maxX - minX + 1 > MAX_XZ || maxZ - minZ + 1 > MAX_XZ || maxY - minY + 1 > MAX_Y || air.size() > MAX_AIR) {
                return null;
            }

            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (air.contains(n)) {
                    continue;
                }
                BlockState s = level.getBlockState(n);
                if (s.isAir()) {
                    air.add(n);
                    queue.add(n);
                    continue;
                }
                if (d == Direction.UP) {
                    if (s.is(ModBlocks.LASER_EMITTER.get()) || s.is(ModBlocks.STEEL_EMITTER.get())) {
                        emitters.add(n);
                    } else if (!s.isSolidRender(level, n)) {
                        return null;
                    }
                } else if (d == Direction.DOWN) {
                    if (!s.isSolidRender(level, n)) {
                        return null;
                    }
                } else {
                    // Side walls: garage_walls blocks, or the controller itself.
                    if (!s.is(WALLS) && !s.is(ModBlocks.GARAGE_CONTROLLER.get()) && !n.equals(controller)) {
                        return null;
                    }
                }
            }
        }

        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        int height = maxY - minY + 1;
        if (width < MIN_XZ || depth < MIN_XZ || height < MIN_Y) {
            return null;
        }
        if (emitters.size() < MIN_EMITTERS || emitters.size() > MAX_EMITTERS) {
            return null;
        }
        return new GarageBay(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), List.copyOf(emitters));
    }

    /** True if any emitter in this bay is a steel emitter. */
    public boolean hasSteelEmitter(Level level) {
        for (BlockPos e : emitters) {
            if (level.getBlockState(e).is(ModBlocks.STEEL_EMITTER.get())) {
                return true;
            }
        }
        return false;
    }

    /** Center of the bay floor, used as the repair target for beams. */
    public BlockPos center() {
        return new BlockPos((min.getX() + max.getX()) / 2, min.getY(), (min.getZ() + max.getZ()) / 2);
    }
}
