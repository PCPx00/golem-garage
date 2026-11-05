package com.xtrip.golemgarage.block;

import com.mojang.serialization.MapCodec;
import com.xtrip.golemgarage.block.entity.FabricatorBlockEntity;
import com.xtrip.golemgarage.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** The Parts Fabricator workbench. */
public class FabricatorBlock extends BaseEntityBlock {
    public static final MapCodec<FabricatorBlock> CODEC = simpleCodec(FabricatorBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public FabricatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FabricatorBlockEntity fab)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!fab.canPlaceItem(0, stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            fab.insert(stack);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof FabricatorBlockEntity fab)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            fab.cycle(player, 1);
            return InteractionResult.SUCCESS;
        }
        ItemStack out = fab.takeOutput();
        if (!out.isEmpty()) {
            if (!player.addItem(out)) {
                player.drop(out, false);
            }
            return InteractionResult.SUCCESS;
        }
        var m = fab.material();
        int need = FabricatorBlockEntity.cost(fab.shape().slot());
        player.displayClientMessage(m == null
                ? Component.translatable("golemgarage.fab.feed", fab.preview().getHoverName(), need)
                : Component.translatable("golemgarage.fab.status", fab.preview().getHoverName(), fab.input().getCount(), need), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FabricatorBlockEntity fab) {
            Containers.dropContents(level, pos, fab);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FabricatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FABRICATOR.get(),
                level.isClientSide ? FabricatorBlockEntity::clientTick : FabricatorBlockEntity::serverTick);
    }
}
