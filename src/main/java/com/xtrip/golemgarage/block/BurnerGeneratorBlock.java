package com.xtrip.golemgarage.block;

import com.mojang.serialization.MapCodec;
import com.xtrip.golemgarage.power.BurnerGeneratorBlockEntity;
import com.xtrip.golemgarage.power.GeneratorBlockEntity;
import com.xtrip.golemgarage.power.PowerBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** The Scrap Burner: a riveted firebox that turns fuel and lava into FE. Right-click to open it. */
public class BurnerGeneratorBlock extends BaseEntityBlock {
    public static final MapCodec<BurnerGeneratorBlock> CODEC = simpleCodec(BurnerGeneratorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public BurnerGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        // A lava bucket pours straight into the tank.
        if (stack.is(Items.LAVA_BUCKET) && level.getBlockEntity(pos) instanceof BurnerGeneratorBlockEntity burner) {
            if (!level.isClientSide && burner.pourBucket()) {
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1f, 1f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // A jerry can fills up from the tank or the fuel slot.
        if (stack.getItem() instanceof com.xtrip.golemgarage.deco.JerryCanItem
                && level.getBlockEntity(pos) instanceof BurnerGeneratorBlockEntity burner) {
            if (!level.isClientSide) {
                int have = com.xtrip.golemgarage.deco.JerryCanItem.fuel(stack);
                int got = burner.fillCan(com.xtrip.golemgarage.deco.JerryCanItem.CAPACITY - have);
                if (got > 0) {
                    com.xtrip.golemgarage.deco.JerryCanItem.setFuel(stack, have + got);
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1f, 1.2f);
                } else {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            have >= com.xtrip.golemgarage.deco.JerryCanItem.CAPACITY ? "golemgarage.jerry_can.full"
                                    : "golemgarage.jerry_can.no_fuel"), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof BurnerGeneratorBlockEntity burner) {
            sp.openMenu(burner, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BurnerGeneratorBlockEntity burner) {
            for (int i = 0; i < burner.items().getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), burner.items().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        Direction f = state.getValue(FACING);
        double x = pos.getX() + 0.5 + f.getStepX() * 0.52, z = pos.getZ() + 0.5 + f.getStepZ() * 0.52;
        double side = (random.nextDouble() - 0.5) * 0.5;
        level.addParticle(ParticleTypes.FLAME, x + f.getStepZ() * side, pos.getY() + 0.3, z + f.getStepX() * side, 0, 0, 0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.45, pos.getZ() + 0.5, 0, 0.04, 0);
        }
        if (random.nextInt(8) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, SoundEvents.FURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS, 0.8f, 0.8f, false);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BurnerGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, PowerBlocks.BURNER_BE.get(), GeneratorBlockEntity::serverTick);
    }
}
