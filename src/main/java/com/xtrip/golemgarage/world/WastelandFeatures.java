package com.xtrip.golemgarage.world;

import com.xtrip.golemgarage.Config;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.block.GarageControllerBlock;
import com.xtrip.golemgarage.block.GarageDoorBlock;
import com.xtrip.golemgarage.block.ModBlocks;
import com.xtrip.golemgarage.entity.ModEntities;
import com.xtrip.golemgarage.entity.RigEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Wasteland finds generated in the world: scrapyards (junk heaps, a salvage crate, sometimes a wrecked rig
 * you can repair) and abandoned garages (a ruined Garage with a broken golem in its crate, waiting to be rebuilt).
 */
public final class WastelandFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, GolemGarage.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SCRAPYARD =
            FEATURES.register("scrapyard", () -> new Scrapyard(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ABANDONED_GARAGE =
            FEATURES.register("abandoned_garage", () -> new AbandonedGarage(NoneFeatureConfiguration.CODEC));

    private WastelandFeatures() {
    }

    /** Ground level at x, z, or MIN_VALUE if it's water or not a solid top. */
    static int ground(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos below = new BlockPos(x, y - 1, z);
        BlockState s = level.getBlockState(below);
        if (!s.getFluidState().isEmpty() || !s.isSolid()) {
            return Integer.MIN_VALUE;
        }
        return y;
    }

    /** True if the square of radius r around (x, z) is dry and within 2 blocks of flat; returns the base height. */
    static int flatBase(WorldGenLevel level, int cx, int cz, int r) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -r; dx <= r; dx += Math.max(1, r / 2)) {
            for (int dz = -r; dz <= r; dz += Math.max(1, r / 2)) {
                int y = ground(level, cx + dx, cz + dz);
                if (y == Integer.MIN_VALUE) {
                    return Integer.MIN_VALUE;
                }
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        return max - min > 2 ? Integer.MIN_VALUE : min;
    }

    static void fill(WorldGenLevel level, BlockPos pos, RandomSource rnd, List<ItemStack> items) {
        if (level.getBlockEntity(pos) instanceof Container c) {
            for (ItemStack s : items) {
                for (int tries = 0; tries < 8; tries++) {
                    int slot = rnd.nextInt(c.getContainerSize());
                    if (c.getItem(slot).isEmpty()) {
                        c.setItem(slot, s);
                        break;
                    }
                }
            }
        }
    }

    static boolean enabled() {
        return Config.get(Config.WORLDGEN::get, true);
    }

    // =================================================================== scrapyard
    static final class Scrapyard extends Feature<NoneFeatureConfiguration> {
        private static final Block[] JUNK = {Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.IRON_BARS,
                Blocks.WEATHERED_COPPER, Blocks.EXPOSED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER, Blocks.SMOOTH_BASALT,
                Blocks.POLISHED_BLACKSTONE, Blocks.CHAIN, Blocks.GRAVEL, Blocks.COARSE_DIRT, Blocks.IRON_BLOCK};

        Scrapyard(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            if (!enabled()) {
                return false;
            }
            WorldGenLevel level = ctx.level();
            RandomSource rnd = ctx.random();
            BlockPos o = ctx.origin();
            int base = flatBase(level, o.getX(), o.getZ(), 6);
            if (base == Integer.MIN_VALUE) {
                return false;
            }
            // Churned ground.
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    if (dx * dx + dz * dz > 49 || rnd.nextInt(5) == 0) {
                        continue;
                    }
                    int y = ground(level, o.getX() + dx, o.getZ() + dz);
                    if (y == Integer.MIN_VALUE) {
                        continue;
                    }
                    Block top = rnd.nextInt(3) == 0 ? Blocks.GRAVEL : rnd.nextBoolean() ? Blocks.COARSE_DIRT : Blocks.ROOTED_DIRT;
                    level.setBlock(new BlockPos(o.getX() + dx, y - 1, o.getZ() + dz), top.defaultBlockState(), 2);
                    BlockPos above = new BlockPos(o.getX() + dx, y, o.getZ() + dz);
                    if (!level.getBlockState(above).isAir() && level.getBlockState(above).canBeReplaced()) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
                    }
                    if (rnd.nextInt(18) == 0) {
                        level.setBlock(above, Blocks.DEAD_BUSH.defaultBlockState(), 2);
                    }
                }
            }
            // Junk heaps.
            int heaps = 3 + rnd.nextInt(3);
            for (int h = 0; h < heaps; h++) {
                int hx = o.getX() + rnd.nextInt(11) - 5;
                int hz = o.getZ() + rnd.nextInt(11) - 5;
                int r = 1 + rnd.nextInt(2);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        int y = ground(level, hx + dx, hz + dz);
                        if (y == Integer.MIN_VALUE) {
                            continue;
                        }
                        int height = Math.max(0, r + 1 - Math.abs(dx) - Math.abs(dz) + rnd.nextInt(2) - 1);
                        for (int k = 0; k < height; k++) {
                            Block b = JUNK[rnd.nextInt(rnd.nextInt(10) == 0 ? JUNK.length : JUNK.length - 1)];
                            level.setBlock(new BlockPos(hx + dx, y + k, hz + dz), b.defaultBlockState(), 2);
                        }
                    }
                }
            }
            // A battered anvil, a cobweb or two and a tyre stack (black wool).
            BlockPos anvil = surface(level, o.offset(rnd.nextInt(9) - 4, 0, rnd.nextInt(9) - 4));
            if (anvil != null) {
                level.setBlock(anvil, Blocks.DAMAGED_ANVIL.defaultBlockState()
                        .setValue(AnvilBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(rnd)), 2);
            }
            BlockPos tyres = surface(level, o.offset(rnd.nextInt(9) - 4, 0, rnd.nextInt(9) - 4));
            if (tyres != null) {
                for (int k = 0; k < 2 + rnd.nextInt(2); k++) {
                    level.setBlock(tyres.above(k), Blocks.BLACK_WOOL.defaultBlockState(), 2);
                }
            }
            for (int k = 0; k < 2; k++) {
                BlockPos web = surface(level, o.offset(rnd.nextInt(11) - 5, 0, rnd.nextInt(11) - 5));
                if (web != null) {
                    level.setBlock(web, Blocks.COBWEB.defaultBlockState(), 2);
                }
            }
            // The salvage crate.
            BlockPos crate = surface(level, o.offset(rnd.nextInt(5) - 2, 0, rnd.nextInt(5) - 2));
            if (crate != null) {
                level.setBlock(crate, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 2);
                fill(level, crate, rnd, Salvage.crate(rnd, false));
            }
            // Sometimes a wrecked rig: no fuel, badly hurt, but it can be fixed.
            if (rnd.nextInt(3) == 0) {
                BlockPos at = surface(level, o.offset(rnd.nextInt(7) - 3, 0, rnd.nextInt(7) - 3));
                RigEntity rig = ModEntities.RIG.get().create(level.getLevel());
                if (at != null && rig != null) {
                    rig.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, rnd.nextFloat() * 360, 0);
                    rig.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
                    rig.setVariant(com.xtrip.golemgarage.entity.RigVariant.values()[rnd.nextInt(4)]);
                    rig.setFuel(0);
                    rig.setHealth(8 + rnd.nextInt(10));
                    if (rnd.nextInt(3) == 0) {
                        rig.fit(RigEntity.RAM);
                    }
                    if (rnd.nextInt(4) == 0) {
                        rig.fit(RigEntity.PLATING);
                    }
                    level.addFreshEntity(rig);
                }
            }
            return true;
        }
    }

    /** The air block just above the ground at x, z (near the given position), or null. */
    static BlockPos surface(WorldGenLevel level, BlockPos near) {
        int y = ground(level, near.getX(), near.getZ());
        if (y == Integer.MIN_VALUE) {
            return null;
        }
        BlockPos p = new BlockPos(near.getX(), y, near.getZ());
        return level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced() ? p : null;
    }

    // =================================================================== abandoned garage
    static final class AbandonedGarage extends Feature<NoneFeatureConfiguration> {
        AbandonedGarage(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            if (!enabled()) {
                return false;
            }
            WorldGenLevel level = ctx.level();
            RandomSource rnd = ctx.random();
            BlockPos o = ctx.origin();
            int base = flatBase(level, o.getX(), o.getZ(), 4);
            if (base == Integer.MIN_VALUE) {
                return false;
            }
            BlockPos min = new BlockPos(o.getX() - 3, base, o.getZ() - 3);
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(rnd);
            // 7 x 7 outside, 5 x 5 inside, 4 tall inside.
            for (int dx = 0; dx < 7; dx++) {
                for (int dz = 0; dz < 7; dz++) {
                    for (int dy = -1; dy <= 4; dy++) {
                        BlockPos p = min.offset(dx, dy, dz);
                        boolean edge = dx == 0 || dx == 6 || dz == 0 || dz == 6;
                        boolean floor = dy == -1;
                        boolean roof = dy == 4;
                        BlockState s;
                        if (floor) {
                            s = (rnd.nextInt(4) == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.SMOOTH_STONE).defaultBlockState();
                        } else if (edge || roof) {
                            // Ruined: the higher up, the more is missing.
                            int missing = roof ? 45 : dy * 12;
                            if (rnd.nextInt(100) < missing) {
                                s = Blocks.AIR.defaultBlockState();
                            } else {
                                int r = rnd.nextInt(10);
                                s = (r < 4 ? Blocks.STONE_BRICKS : r < 7 ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS)
                                        .defaultBlockState();
                            }
                        } else {
                            s = Blocks.AIR.defaultBlockState();
                        }
                        level.setBlock(p, s, 2);
                    }
                }
            }
            // Clear the space above the ruin.
            for (int dx = 0; dx < 7; dx++) {
                for (int dz = 0; dz < 7; dz++) {
                    for (int dy = 5; dy < 9; dy++) {
                        BlockPos p = min.offset(dx, dy, dz);
                        if (!level.getBlockState(p).isAir() && !level.getBlockState(p).is(net.minecraft.tags.BlockTags.LOGS)) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
            BlockPos center = min.offset(3, 0, 3);
            // The controller, still in its wall, and one emitter left in the roof: you need another.
            BlockPos ctrl = center.relative(facing.getOpposite(), 3);
            level.setBlock(ctrl, ModBlocks.GARAGE_CONTROLLER.get().defaultBlockState()
                    .setValue(GarageControllerBlock.FACING, facing), 2);
            level.setBlock(center.above(4).relative(facing.getClockWise()), ModBlocks.LASER_EMITTER.get().defaultBlockState(), 2);
            // A stuck roller door on the far side.
            BlockPos door = center.relative(facing, 3);
            for (int dy = 0; dy < 2; dy++) {
                level.setBlock(door.above(dy), ModBlocks.GARAGE_DOOR.get().defaultBlockState()
                        .setValue(GarageDoorBlock.FACING, facing).setValue(GarageDoorBlock.OPEN, false), 2);
            }
            level.setBlock(door.above(2), Blocks.AIR.defaultBlockState(), 2);
            // Cobwebs, a work table and the crate with a broken golem in it.
            for (int k = 0; k < 3; k++) {
                BlockPos web = center.offset(rnd.nextInt(5) - 2, 1 + rnd.nextInt(3), rnd.nextInt(5) - 2);
                if (level.getBlockState(web).isAir()) {
                    level.setBlock(web, Blocks.COBWEB.defaultBlockState(), 2);
                }
            }
            BlockPos corner = center.relative(facing.getClockWise(), 2).relative(facing.getOpposite(), 2);
            level.setBlock(corner, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
            fill(level, corner, rnd, Salvage.crate(rnd, true));
            BlockPos table = center.relative(facing.getCounterClockWise(), 2).relative(facing.getOpposite(), 2);
            level.setBlock(table, Blocks.SMITHING_TABLE.defaultBlockState(), 2);
            level.setBlock(center.relative(facing.getCounterClockWise(), 2).relative(facing, 2), Blocks.BARREL.defaultBlockState(), 2);
            fill(level, center.relative(facing.getCounterClockWise(), 2).relative(facing, 2), rnd, Salvage.crate(rnd, false));
            return true;
        }
    }
}
