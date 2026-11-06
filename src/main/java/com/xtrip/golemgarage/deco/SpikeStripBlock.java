package com.xtrip.golemgarage.deco;

import com.xtrip.golemgarage.entity.CubEntity;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.entity.RigEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A strip of welded spikes dropped from the back of a rig. Mobs and raiders' rigs that cross it are hurt and slowed.
 * Players, your golems, Cubs and your own rigs roll over it safely. It rusts away after 30 seconds.
 */
public class SpikeStripBlock extends Block {
    public static final int LIFETIME = 600;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public SpikeStripBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, LIFETIME);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd) {
        level.removeBlock(pos, false);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity le) || safe(le)) {
            return;
        }
        le.makeStuckInBlock(state, new Vec3(0.45, 1, 0.45));
        if (le.tickCount % 10 == 0) {
            le.hurt(level.damageSources().cactus(), le instanceof RigEntity ? 6f : 3f);
        }
    }

    private static boolean safe(LivingEntity e) {
        if (e instanceof RigEntity r) {
            return !r.isRogue();
        }
        if (e instanceof GolemEntity g) {
            return !g.isRogue();
        }
        return e instanceof Player || e instanceof CubEntity;
    }
}
