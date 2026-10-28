package com.xtrip.golemgarage.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A Laser Emitter with a steel core. A bay with at least one steel emitter repairs 20% faster.
 * It counts as an emitter for bay validation, the same as a Laser Emitter.
 */
public class SteelEmitterBlock extends LaserEmitterBlock {
    public SteelEmitterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
