package com.xtrip.golemgarage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A ceiling-mounted repair laser for the Garage. Place 2-4 in the Garage ceiling.
 * During a repair the controller draws its aiming head and beam.
 */
public class LaserEmitterBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1, 8, 1, 15, 16, 15);

    public LaserEmitterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
