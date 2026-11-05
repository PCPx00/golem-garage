package com.xtrip.golemgarage.power;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Up to 40 FE/t under open sky, following the sun: most at noon, a trickle at dawn and dusk, nothing at night.
 * Rain cuts it to a third, a thunderstorm to a fifth.
 */
public class SolarPanelBlockEntity extends GeneratorBlockEntity {
    public static final int PEAK = 40;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(PowerBlocks.SOLAR_PANEL_BE.get(), pos, state, 20_000, 200);
    }

    @Override
    protected int produce(Level level) {
        if (!level.dimensionType().hasSkyLight() || !level.canSeeSky(worldPosition.above())) {
            return 0;
        }
        long t = Math.floorMod(level.getDayTime(), 24000L);
        if (t > 12700) {
            return 0;
        }
        float sun = Math.max(0.15f, Mth.sin((float) Math.PI * t / 12700f));
        if (level.isThundering()) {
            sun *= 0.2f;
        } else if (level.isRaining()) {
            sun *= 0.35f;
        }
        return Math.max(1, Math.round(PEAK * sun));
    }
}
