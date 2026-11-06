package com.xtrip.golemgarage.deco;

import com.xtrip.golemgarage.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A stack of old tyres, one to four high. Use a tyre on it to add one, or an empty hand to take one off. It drops
 * its tyres when broken.
 */
public class TyreStackBlock extends Block {
    public static final IntegerProperty TYRES = IntegerProperty.create("tyres", 1, 4);
    private static final VoxelShape[] SHAPES = new VoxelShape[5];

    static {
        for (int i = 1; i <= 4; i++) {
            SHAPES[i] = Block.box(1, 0, 1, 15, i * 4, 15);
        }
    }

    public TyreStackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TYRES, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYRES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(TYRES)];
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModItems.TYRE.get()) && state.getValue(TYRES) < 4) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(TYRES, state.getValue(TYRES) + 1), 3);
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 0.6f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            int n = state.getValue(TYRES);
            if (n > 1) {
                level.setBlock(pos, state.setValue(TYRES, n - 1), 3);
            } else {
                level.removeBlock(pos, false);
            }
            if (!player.addItem(new ItemStack(ModItems.TYRE.get()))) {
                player.drop(new ItemStack(ModItems.TYRE.get()), false);
            }
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1f, 0.6f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
