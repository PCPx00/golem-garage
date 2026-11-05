package com.xtrip.golemgarage.power;

import com.xtrip.golemgarage.block.WindTurbineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A scrap windmill on a mast. It makes more the higher it stands (6 FE/t near sea level up to 70 FE/t high up),
 * half again in rain and double in a storm. Blocks around the rotor steal its wind. Nothing underground or in the
 * Nether and the End.
 */
public class WindTurbineBlockEntity extends GeneratorBlockEntity {
    /** How free the rotor's wind is, 0 to 1, rechecked every few seconds. */
    private float clear = 1f;
    /** Client: rotor angle in degrees, and its spin speed. */
    public float angle, angleO, spin;

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(PowerBlocks.WIND_TURBINE_BE.get(), pos, state, 32_000, 300);
    }

    public float clearance() {
        return clear;
    }

    @Override
    protected int produce(Level level) {
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling() || !level.dimensionType().natural()) {
            return 0;
        }
        if (level.getGameTime() % 100 == (worldPosition.asLong() & 63) || level.getGameTime() < 5) {
            clear = measureClearance(level);
        }
        float base = Mth.clamp((worldPosition.getY() - 58) * 1.1f + 6, 6, 70);
        if (level.isThundering()) {
            base *= 2f;
        } else if (level.isRaining()) {
            base *= 1.5f;
        }
        // A slow gusting wobble so the output feels alive.
        float gust = 0.85f + 0.15f * Mth.sin((level.getGameTime() + worldPosition.getX() * 7) * 0.01f);
        return Math.round(base * clear * gust);
    }

    /** Counts solid blocks in the rotor's disc (3 above the base) and the mast's column. */
    private float measureClearance(Level level) {
        Direction facing = getBlockState().getValue(WindTurbineBlock.FACING);
        Direction across = facing.getClockWise();
        BlockPos hub = worldPosition.above(3);
        int total = 0, blocked = 0;
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                if (Math.abs(a) == 2 && Math.abs(b) == 2) {
                    continue;
                }
                BlockPos p = hub.relative(across, a).above(b);
                total++;
                if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                    blocked++;
                }
            }
        }
        for (int y = 1; y <= 2; y++) {
            total++;
            if (!level.getBlockState(worldPosition.above(y)).isAir()) {
                blocked += 3;
            }
        }
        return Math.max(0f, 1f - blocked / (float) total * 1.5f);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, WindTurbineBlockEntity t) {
        t.angleO = t.angle;
        float target = t.rate * 0.35f;
        t.spin += (target - t.spin) * 0.02f;
        t.angle = (t.angle + t.spin + (t.rate > 0 ? 0.3f : 0)) % 360f;
        if (t.angle < t.angleO) {
            t.angleO -= 360f;
        }
    }
}
