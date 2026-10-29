package com.xtrip.golemgarage.block;

import com.mojang.serialization.MapCodec;
import com.xtrip.golemgarage.block.entity.GarageDoorBlockEntity;
import com.xtrip.golemgarage.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A roller door for the Garage. Place several side by side and on top of each other to make one big door:
 * they open and close together. It opens by itself for golems and cubs, with redstone, or when right-clicked
 * (which keeps it open until it is clicked again). It counts as Garage wall, open or shut.
 */
public class GarageDoorBlock extends BaseEntityBlock {
    public static final MapCodec<GarageDoorBlock> CODEC = simpleCodec(GarageDoorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    private static final VoxelShape PANEL_Z = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape PANEL_X = Block.box(6, 0, 0, 10, 16, 16);
    private static final VoxelShape TOP_Z = Block.box(0, 13, 4, 16, 16, 12);
    private static final VoxelShape TOP_X = Block.box(4, 13, 0, 12, 16, 16);

    public GarageDoorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    /** Joins an existing door next to it (same facing, same open state), or faces the player. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.getBlock() instanceof GarageDoorBlock
                    && (d.getAxis() == Direction.Axis.Y || d.getAxis() == n.getValue(FACING).getClockWise().getAxis())) {
                return defaultBlockState().setValue(FACING, n.getValue(FACING)).setValue(OPEN, n.getValue(OPEN));
            }
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private static boolean alongX(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (state.getValue(OPEN)) {
            return alongX(state) ? TOP_Z : TOP_X;
        }
        return alongX(state) ? PANEL_Z : PANEL_X;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (state.getValue(OPEN)) {
            return Shapes.empty();
        }
        return alongX(state) ? PANEL_Z : PANEL_X;
    }

    /** Golems plan their walk straight through: the door rolls up for them as they get close. */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.LAND || state.getValue(OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            GarageDoorBlockEntity.toggleHeld(level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GarageDoorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.GARAGE_DOOR.get(),
                level.isClientSide ? GarageDoorBlockEntity::clientTick : GarageDoorBlockEntity::serverTick);
    }
}
