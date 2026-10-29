package com.xtrip.golemgarage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A ceiling-mounted repair laser for the Garage. Place 2-4 in the Garage ceiling.
 * During a repair the controller draws its aiming head and beam. It glows brighter while its Garage is complete.
 */
public class LaserEmitterBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1, 8, 1, 15, 16, 15);

    /** Lit by the controller while the Garage it belongs to is complete. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LaserEmitterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
