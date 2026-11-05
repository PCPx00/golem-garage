package com.xtrip.golemgarage.power;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The Scrap Burner's screen: a fuel slot, an empty-bucket slot, and the player's inventory. */
public class BurnerMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final BlockPos pos;

    public BurnerMenu(int id, Inventory inv, IItemHandler items, ContainerData data, BlockPos pos) {
        super(PowerBlocks.BURNER_MENU.get(), id);
        this.data = data;
        this.pos = pos;
        addSlot(new SlotItemHandler(items, BurnerGeneratorBlockEntity.FUEL, 44, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BurnerGeneratorBlockEntity.isFuel(stack);
            }
        });
        addSlot(new SlotItemHandler(items, BurnerGeneratorBlockEntity.BUCKETS, 44, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new net.minecraft.world.inventory.Slot(inv, 9 + r * 9 + c, 8 + c * 18, 92 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new net.minecraft.world.inventory.Slot(inv, c, 8 + c * 18, 150));
        }
        addDataSlots(data);
    }

    public static BurnerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new BurnerMenu(id, inv, new ItemStackHandler(2), new SimpleContainerData(6), buf.readBlockPos());
    }

    public int energy() {
        return (data.get(0) & 0xFFFF) | (data.get(1) << 16);
    }

    public int lava() {
        return data.get(2);
    }

    public int burn() {
        return data.get(3);
    }

    public int burnMax() {
        return data.get(4);
    }

    public int rate() {
        return data.get(5);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64
                && player.level().getBlockState(pos).getBlock() instanceof com.xtrip.golemgarage.block.BurnerGeneratorBlock;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (BurnerGeneratorBlockEntity.isFuel(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
