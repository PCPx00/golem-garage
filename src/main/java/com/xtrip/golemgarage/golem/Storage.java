package com.xtrip.golemgarage.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * Item storage through NeoForge's item handler capability, so golems work with chests, barrels and any
 * modded storage that exposes items (drawers, crates, AE2 interfaces, Create vaults, Grim Storage...).
 */
public final class Storage {
    /** The storage at a position, from any side, or null if the block holds no items. */
    @Nullable
    public static IItemHandler at(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) == null) {
            return null;
        }
        IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (h != null) {
            return h;
        }
        for (Direction d : Direction.values()) {
            h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, d);
            if (h != null) {
                return h;
            }
        }
        return null;
    }

    public static int count(@Nullable IItemHandler h) {
        if (h == null) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < h.getSlots(); i++) {
            n += h.getStackInSlot(i).getCount();
        }
        return n;
    }

    /** True when nothing more can go in (tests a plain item so it works for filtered storage too). */
    public static boolean isFull(@Nullable IItemHandler h) {
        if (h == null) {
            return false;
        }
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty() || s.getCount() < Math.min(s.getMaxStackSize(), h.getSlotLimit(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Moves one stack out of the storage into a receiver. {@code receive} takes a stack and returns what didn't fit.
     * Returns true if anything moved.
     */
    public static boolean takeOne(IItemHandler from, UnaryOperator<ItemStack> receive) {
        for (int i = 0; i < from.getSlots(); i++) {
            ItemStack peek = from.extractItem(i, 64, true);
            if (peek.isEmpty()) {
                continue;
            }
            ItemStack rest = receive.apply(peek.copy());
            int moved = peek.getCount() - rest.getCount();
            if (moved > 0) {
                from.extractItem(i, moved, false);
                return true;
            }
        }
        return false;
    }

    /** Inserts as much of a stack as fits, filling matching stacks first. Returns the remainder. */
    public static ItemStack insert(IItemHandler into, ItemStack stack) {
        return ItemHandlerHelper.insertItemStacked(into, stack, false);
    }

    private Storage() {
    }
}
