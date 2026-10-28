package com.xtrip.golemgarage.menu;

import com.xtrip.golemgarage.entity.CubEntity;
import com.xtrip.golemgarage.item.OutfitItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** The Cub's pockets (9 slots) and wardrobe (head, body, accessory). */
public class CubMenu extends AbstractContainerMenu {
    public static final int POCKETS = 9;
    public static final int WARDROBE = 3;

    private final Container pockets;
    private final Container wardrobe;
    @Nullable
    private final CubEntity cub;
    private final int cubId;

    public CubMenu(int id, Inventory inv, Container pockets, Container wardrobe, @Nullable CubEntity cub, int cubId) {
        super(ModMenus.CUB.get(), id);
        this.pockets = pockets;
        this.wardrobe = wardrobe;
        this.cub = cub;
        this.cubId = cubId;
        for (int i = 0; i < WARDROBE; i++) {
            final OutfitItem.Slot kind = OutfitItem.Slot.values()[i];
            addSlot(new Slot(wardrobe, i, 62, 18 + i * 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof OutfitItem o && o.slot() == kind;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int i = 0; i < POCKETS; i++) {
            addSlot(new Slot(pockets, i, 8 + i * 18, 90));
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inv, 9 + r * 9 + c, 8 + c * 18, 114 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inv, c, 8 + c * 18, 172));
        }
    }

    public static CubMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        int cubId = buf.readVarInt();
        CubEntity cub = inv.player.level().getEntity(cubId) instanceof CubEntity c ? c : null;
        return new CubMenu(id, inv, new SimpleContainer(POCKETS), new SimpleContainer(WARDROBE), cub, cubId);
    }

    /** The Cub being dressed (on the client, for the preview). */
    @Nullable
    public CubEntity cub() {
        return cub;
    }

    @Override
    public boolean stillValid(Player player) {
        return cub == null || (cub.isAlive() && cub.distanceToSqr(player) < 64);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int cubEnd = WARDROBE + POCKETS;
        int end = slots.size();
        if (index < cubEnd) {
            if (!moveItemStackTo(stack, cubEnd, end, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            if (stack.getItem() instanceof OutfitItem o) {
                moved = moveItemStackTo(stack, o.slot().ordinal(), o.slot().ordinal() + 1, false);
            }
            if (!moved && !moveItemStackTo(stack, WARDROBE, cubEnd, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
