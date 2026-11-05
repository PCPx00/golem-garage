package com.xtrip.golemgarage.block;

import com.xtrip.golemgarage.power.PowerBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The invisible solid part of a Wind Turbine's mast, standing in the three blocks above its base so players and
 * mobs bump into the lattice instead of walking through it. Breaking any of it breaks the whole turbine.
 */
public class TurbineMastBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public TurbineMastBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** The mast stands only while there is a turbine (or more mast) below it. */
    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        if (dir == Direction.DOWN && !canSurvive(state, level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) || below.is(PowerBlocks.WIND_TURBINE.get());
    }

    /** Breaking the mast breaks the turbine's base, which drops the item. */
    @Override
    public BlockState playerWillDestroy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos p = pos.below();
        while (level.getBlockState(p).is(this)) {
            p = p.below();
        }
        if (level.getBlockState(p).is(PowerBlocks.WIND_TURBINE.get())) {
            level.destroyBlock(p, !player.getAbilities().instabuild, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(PowerBlocks.WIND_TURBINE_ITEM.get());
    }
}
