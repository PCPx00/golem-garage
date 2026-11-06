package com.xtrip.golemgarage.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A Garage Workbench: a scrap-metal bench with a vice and a tool board. Works as a crafting table. */
public class WorkbenchBlock extends DecoBlock {
    public WorkbenchBlock(Properties properties) {
        super(properties, 0, 0, 0, 16, 16, 16);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.openMenu(getMenuProvider(state, level, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(level, pos)) {
            @Override
            public boolean stillValid(Player player) {
                return player.distanceToSqr(pos.getCenter()) <= 64 && level.getBlockState(pos).getBlock() instanceof WorkbenchBlock;
            }
        }, Component.translatable("block.golemgarage.workbench"));
    }
}
