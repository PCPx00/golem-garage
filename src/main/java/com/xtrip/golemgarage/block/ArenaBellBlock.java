package com.xtrip.golemgarage.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** The Arena Bell: a scrap gong on a post. Ring it with two golems inside its ring and they spar. */
public class ArenaBellBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public ArenaBellBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
            String err = com.xtrip.golemgarage.world.Arena.ring(sl, pos, player);
            if (err != null) {
                player.displayClientMessage(Component.translatable(err).withStyle(ChatFormatting.RED), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** A faint ring of dust marks the arena's edge. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double a = random.nextDouble() * Math.PI * 2;
        level.addParticle(new DustParticleOptions(new Vector3f(1f, 0.75f, 0.2f), 1f),
                pos.getX() + 0.5 + Math.cos(a) * com.xtrip.golemgarage.world.Arena.RING, pos.getY() + 0.1,
                pos.getZ() + 0.5 + Math.sin(a) * com.xtrip.golemgarage.world.Arena.RING, 0, 0, 0);
    }
}
