package com.xtrip.golemgarage.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A decoration that faces the player who placed it. The shape is given for a block facing north (x0..x1, z0..z1 in
 * pixels) and turned to match.
 */
public class DecoBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final VoxelShape north, east, south, west;

    public DecoBlock(Properties properties, double x0, double y0, double z0, double x1, double y1, double z1) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        north = Block.box(x0, y0, z0, x1, y1, z1);
        south = Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
        east = Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
        west = Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
    }

    public DecoBlock(Properties properties) {
        this(properties, 0, 0, 0, 16, 16, 16);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case EAST -> east;
            case SOUTH -> south;
            case WEST -> west;
            default -> north;
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    static VoxelShape full() {
        return Shapes.block();
    }
}
