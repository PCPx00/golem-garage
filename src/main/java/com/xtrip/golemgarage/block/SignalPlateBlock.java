package com.xtrip.golemgarage.block;

import com.xtrip.golemgarage.entity.GolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A plate that gives a redstone signal while a golem running the "emit redstone" action stands on
 * or right above it. Lets golems open doors, ring bells, or tell your contraptions they're done.
 */
public class SignalPlateBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape UP = Block.box(1, 0, 1, 15, 1, 15);
    private static final VoxelShape DOWN = Block.box(1, 0, 1, 15, 0.5, 15);

    public SignalPlateBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(POWERED) ? DOWN : UP;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return canSupportRigidBlock(level, below) || canSupportCenter(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return dir == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        AABB area = new AABB(pos).inflate(0.4, 0, 0.4).expandTowards(0, 2, 0);
        boolean on = !level.getEntitiesOfClass(GolemEntity.class, area, GolemEntity::isEmittingSignal).isEmpty();
        if (on != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, on), 3);
            if (GolemEntity.DEBUG) {
                com.xtrip.golemgarage.GolemGarage.LOGGER.info("[signal plate {}] powered={}", pos.toShortString(), on);
            }
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
        }
        level.scheduleTick(pos, this, 10);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock()) && state.getValue(POWERED)) {
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        return dir == Direction.UP && state.getValue(POWERED) ? 15 : 0;
    }
}
