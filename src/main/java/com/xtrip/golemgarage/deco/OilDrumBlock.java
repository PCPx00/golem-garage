package com.xtrip.golemgarage.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A dented oil drum. Light it with flint and steel (or a fire charge) for a burn barrel: a fire on top that lights
 * up the yard. A shovel or a water bucket puts it out. Standing on a lit barrel burns.
 */
public class OilDrumBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public OilDrumBlock(Properties properties) {
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

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        boolean lit = state.getValue(LIT);
        if (!lit && (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(LIT, true), 3);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1f, 1f);
                if (stack.is(Items.FIRE_CHARGE)) {
                    stack.consume(1, player);
                } else {
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (lit && (stack.is(net.minecraft.tags.ItemTags.SHOVELS) || stack.is(Items.WATER_BUCKET))) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(LIT, false), 3);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1f, 1f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (state.getValue(LIT) && entity instanceof LivingEntity && !entity.isSteppingCarefully()) {
            entity.hurt(level.damageSources().hotFloor(), 1f);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FLAME, x + (rnd.nextDouble() - 0.5) * 0.5, y, z + (rnd.nextDouble() - 0.5) * 0.5,
                    0, 0.03, 0);
        }
        if (rnd.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.4, z, 0, 0.05, 0);
        }
        if (rnd.nextInt(12) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.8f, 1f, false);
        }
    }
}
