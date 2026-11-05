package com.xtrip.golemgarage.item;

import com.xtrip.golemgarage.block.GarageControllerBlock;
import com.xtrip.golemgarage.block.GarageStyle;
import com.xtrip.golemgarage.block.ModBlocks;
import com.xtrip.golemgarage.block.entity.GarageControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * A Garage Blueprint. Sneak + use it on a Garage Controller to copy that Garage's size and style. Use a copied
 * blueprint on another controller to give it the same plan, or on the ground to put down a controller from your
 * inventory with the plan already set, and start building it if you carry the blocks.
 */
public class GarageBlueprintItem extends Item {
    public GarageBlueprintItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static CompoundTag plan(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? null : d.copyTag().contains("PlanW") ? d.copyTag() : null;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof GarageControllerBlockEntity g) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            CompoundTag p = plan(stack);
            if (player.isSecondaryUseActive() || p == null) {
                CompoundTag t = new CompoundTag();
                t.putInt("PlanW", g.planW());
                t.putInt("PlanD", g.planD());
                t.putInt("PlanH", g.planH());
                t.putString("Style", g.style().id());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
                player.displayClientMessage(Component.translatable("golemgarage.blueprint.copied", g.planW(), g.planD(), g.planH(),
                        Component.translatable(g.style().key())).withStyle(ChatFormatting.AQUA), true);
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
            } else {
                apply(g, p);
                player.displayClientMessage(Component.translatable("golemgarage.blueprint.applied").withStyle(ChatFormatting.AQUA), true);
                level.playSound(null, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 1f, 1f);
            }
            return InteractionResult.CONSUME;
        }
        CompoundTag p = plan(stack);
        if (p == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("golemgarage.blueprint.empty").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.FAIL;
        }
        // Stamp it down: a controller from the inventory, facing the way the player looks.
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        int slot = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.GARAGE_CONTROLLER.get())) {
                slot = i;
                break;
            }
        }
        if (slot < 0 && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("golemgarage.blueprint.need_controller").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        BlockPos at = level.getBlockState(pos).canBeReplaced() ? pos : pos.relative(ctx.getClickedFace());
        if (!level.getBlockState(at).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        BlockState state = ModBlocks.GARAGE_CONTROLLER.get().defaultBlockState()
                .setValue(GarageControllerBlock.FACING, player.getDirection());
        sl.setBlock(at, state, 3);
        if (slot >= 0 && !player.getAbilities().instabuild) {
            player.getInventory().getItem(slot).shrink(1);
        }
        if (sl.getBlockEntity(at) instanceof GarageControllerBlockEntity g) {
            apply(g, p);
            String err = g.autoBuild(player);
            player.displayClientMessage(err == null ? Component.translatable("golemgarage.planner.started").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("golemgarage.blueprint.placed").withStyle(ChatFormatting.YELLOW), true);
        }
        sl.playSound(null, at, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6f, 1.2f);
        return InteractionResult.CONSUME;
    }

    private static void apply(GarageControllerBlockEntity g, CompoundTag p) {
        GarageStyle st = GarageStyle.WASTELAND;
        for (GarageStyle s : GarageStyle.values()) {
            if (s.id().equals(p.getString("Style"))) {
                st = s;
            }
        }
        g.setStyle(st);
        g.setPlan(p.getInt("PlanW"), p.getInt("PlanD"), p.getInt("PlanH"), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag p = plan(stack);
        if (p == null) {
            tooltip.add(Component.translatable("golemgarage.blueprint.tip_empty").withStyle(ChatFormatting.GRAY));
        } else {
            String style = "golemgarage.style." + p.getString("Style");
            tooltip.add(Component.translatable("golemgarage.blueprint.plan", p.getInt("PlanW"), p.getInt("PlanD"), p.getInt("PlanH"),
                    Component.translatable(style)).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("golemgarage.blueprint.tip").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return plan(stack) != null;
    }
}
