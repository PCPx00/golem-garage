package com.xtrip.golemgarage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A little round bed. At night a cub nearby curls up on it and sleeps till morning. */
public class CubBedBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 5, 15);

    public CubBedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
