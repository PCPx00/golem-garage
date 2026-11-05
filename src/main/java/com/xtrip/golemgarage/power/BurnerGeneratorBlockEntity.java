package com.xtrip.golemgarage.power;

import com.xtrip.golemgarage.block.BurnerGeneratorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Scrap Burner: burns anything a furnace would (60 FE/t) or lava (120 FE/t, 50 mB every 5 seconds, so a
 * bucket lasts 100 seconds). Pipes can feed it fuel and lava and take the empty buckets out.
 */
public class BurnerGeneratorBlockEntity extends GeneratorBlockEntity implements MenuProvider {
    public static final int ITEM_RATE = 60, LAVA_RATE = 120, TANK = 8000, LAVA_SIP = 50, LAVA_TICKS = 100;
    public static final int FUEL = 0, BUCKETS = 1;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FUEL && isFuel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTank tank = new FluidTank(TANK, fs -> fs.getFluid().is(FluidTags.LAVA)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int burn, burnMax, burnRate;

    /** Pipes: fuel goes in slot 0, empty buckets come out of slot 1. */
    private final IItemHandler pipeItems = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == FUEL ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == BUCKETS ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FUEL && isFuel(stack);
        }
    };

    /** Pipes: lava goes in, nothing comes out. */
    private final IFluidHandler pipeFluid = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int t) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int t) {
            return TANK;
        }

        @Override
        public boolean isFluidValid(int t, FluidStack stack) {
            return stack.getFluid().is(FluidTags.LAVA);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    /** What the screen shows: energy (two halves), lava, burn left, burn total, rate. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> stored() & 0xFFFF;
                case 1 -> stored() >>> 16;
                case 2 -> tank.getFluidAmount();
                case 3 -> burn;
                case 4 -> burnMax;
                case 5 -> rate;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public BurnerGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(PowerBlocks.BURNER_BE.get(), pos, state, 100_000, 400);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(Items.LAVA_BUCKET) || stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    public IItemHandler pipeItems() {
        return pipeItems;
    }

    public IFluidHandler pipeFluid() {
        return pipeFluid;
    }

    public ItemStackHandler items() {
        return items;
    }

    public int lava() {
        return tank.getFluidAmount();
    }

    /** Pours a lava bucket in by hand. */
    public boolean pourBucket() {
        if (TANK - tank.getFluidAmount() < 1000) {
            return false;
        }
        tank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    @Override
    protected int produce(Level level) {
        // Lava buckets in the fuel slot are poured into the tank, the empties set aside.
        ItemStack fuel = items.getStackInSlot(FUEL);
        if (fuel.is(Items.LAVA_BUCKET) && TANK - tank.getFluidAmount() >= 1000
                && items.insertItem(BUCKETS, new ItemStack(Items.BUCKET), true).isEmpty()) {
            tank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
            items.extractItem(FUEL, 1, false);
            items.insertItem(BUCKETS, new ItemStack(Items.BUCKET), false);
        }
        if (burn <= 0 && !full()) {
            if (tank.getFluidAmount() >= LAVA_SIP) {
                tank.drain(LAVA_SIP, IFluidHandler.FluidAction.EXECUTE);
                burn = burnMax = LAVA_TICKS;
                burnRate = LAVA_RATE;
            } else {
                fuel = items.getStackInSlot(FUEL);
                int t = fuel.is(Items.LAVA_BUCKET) ? 0 : fuel.getBurnTime(RecipeType.SMELTING);
                ItemStack rest = fuel.getCraftingRemainingItem();
                if (t > 0 && (rest.isEmpty() || items.insertItem(BUCKETS, rest, true).isEmpty())) {
                    items.extractItem(FUEL, 1, false);
                    if (!rest.isEmpty()) {
                        items.insertItem(BUCKETS, rest.copy(), false);
                    }
                    burn = burnMax = t;
                    burnRate = ITEM_RATE;
                }
            }
        }
        boolean lit = burn > 0;
        if (getBlockState().getValue(BurnerGeneratorBlock.LIT) != lit) {
            level.setBlock(worldPosition, getBlockState().setValue(BurnerGeneratorBlock.LIT, lit), 3);
        }
        if (burn <= 0) {
            return 0;
        }
        // Holds its fire while the buffer is full instead of wasting fuel.
        if (full()) {
            return 0;
        }
        burn--;
        return burnRate;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Burn", burn);
        tag.putInt("BurnMax", burnMax);
        tag.putInt("BurnRate", burnRate);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        burn = tag.getInt("Burn");
        burnMax = tag.getInt("BurnMax");
        burnRate = tag.getInt("BurnRate");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.golemgarage.burner_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BurnerMenu(id, inv, items, data, worldPosition);
    }
}
