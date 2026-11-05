package com.xtrip.golemgarage.menu;

import com.xtrip.golemgarage.block.entity.GarageControllerBlockEntity;
import com.xtrip.golemgarage.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Garage's showcase: golem items put here stand on display shelves inside the finished Garage. */
public class ShowcaseMenu extends AbstractContainerMenu {
    public static final int SLOTS = GarageControllerBlockEntity.SHOWCASE_MAX;
    private final Container showcase;
    private final ContainerData data;
    private final BlockPos pos;

    public ShowcaseMenu(int id, Inventory inv, Container showcase, ContainerData data, BlockPos pos) {
        super(ModMenus.SHOWCASE.get(), id);
        this.showcase = showcase;
        this.data = data;
        this.pos = pos;
        int x0 = (176 - SLOTS * 22) / 2 + 2;
        for (int i = 0; i < SLOTS; i++) {
            final int index = i;
            addSlot(new Slot(showcase, i, x0 + i * 22, 30) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return index < unlocked() && stack.is(ModItems.GOLEM.get());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inv, 9 + r * 9 + c, 8 + c * 18, 92 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inv, c, 8 + c * 18, 150));
        }
        addDataSlots(data);
    }

    public static ShowcaseMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new ShowcaseMenu(id, inv, new SimpleContainer(SLOTS), new SimpleContainerData(1), buf.readBlockPos());
    }

    /** How many slots this Garage's size allows. */
    public int unlocked() {
        return Math.max(1, data.get(0));
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64
                && player.level().getBlockEntity(pos) instanceof GarageControllerBlockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.GOLEM.get())) {
            if (!moveItemStackTo(stack, 0, Math.min(SLOTS, unlocked()), false)) {
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
