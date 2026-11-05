package com.xtrip.golemgarage.power;

import com.xtrip.golemgarage.block.BatteryBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The Scrap Battery: stores 1,000,000 FE. It takes power on every side and sends it out of its front (the side
 * with the plug), so a row of them can't pass energy back and forth. Keeps its charge when broken.
 */
public class BatteryBlockEntity extends BlockEntity {
    public static final int CAPACITY = 1_000_000, RATE = 4_000;
    private final Store store = new Store();
    private int shownLevel = -1;

    final class Store extends EnergyStorage {
        Store() {
            super(CAPACITY, RATE, RATE);
        }

        void set(int n) {
            energy = Math.max(0, Math.min(capacity, n));
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int got = super.receiveEnergy(amount, simulate);
            if (got > 0 && !simulate) {
                setChanged();
            }
            return got;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int got = super.extractEnergy(amount, simulate);
            if (got > 0 && !simulate) {
                setChanged();
            }
            return got;
        }
    }

    /** What pipes see on each side: only the front gives energy out, every side takes it in. */
    private final IEnergyStorage inputOnly = new IEnergyStorage() {
        public int receiveEnergy(int a, boolean s) {
            return store.receiveEnergy(a, s);
        }

        public int extractEnergy(int a, boolean s) {
            return 0;
        }

        public int getEnergyStored() {
            return store.getEnergyStored();
        }

        public int getMaxEnergyStored() {
            return CAPACITY;
        }

        public boolean canExtract() {
            return false;
        }

        public boolean canReceive() {
            return true;
        }
    };

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(PowerBlocks.BATTERY_BE.get(), pos, state);
    }

    public IEnergyStorage side(Direction d) {
        return d == null || d == getBlockState().getValue(BatteryBlock.FACING) ? store : inputOnly;
    }

    public int stored() {
        return store.getEnergyStored();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity b) {
        Direction out = state.getValue(BatteryBlock.FACING);
        int left = Math.min(RATE, b.store.getEnergyStored());
        if (left > 0) {
            IEnergyStorage to = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(out), out.getOpposite());
            if (to != null && to.canReceive()) {
                int sent = to.receiveEnergy(left, false);
                if (sent > 0) {
                    b.store.extractEnergy(sent, false);
                }
            }
        }
        int lvl = Math.round(4f * b.store.getEnergyStored() / CAPACITY);
        if (lvl != b.shownLevel && level.getGameTime() % 10 == 0) {
            b.shownLevel = lvl;
            if (state.getValue(BatteryBlock.LEVEL) != lvl) {
                level.setBlock(pos, state.setValue(BatteryBlock.LEVEL, lvl), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", store.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        store.set(tag.getInt("Energy"));
    }

    // The charge travels with the item.
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        CompoundTag t = new CompoundTag();
        t.putInt("Energy", store.getEnergyStored());
        components.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CustomData d = input.get(DataComponents.CUSTOM_DATA);
        if (d != null) {
            store.set(d.copyTag().getInt("Energy"));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Energy");
    }
}
