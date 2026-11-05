package com.xtrip.golemgarage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * The Golem Dock's energy buffer. Any mod's cables (Forge Energy) can fill it; a golem resting on a charged dock
 * tops up much faster, drawing FE from here.
 */
public class GolemDockBlockEntity extends BlockEntity {
    public static final int CAPACITY = 100_000, MAX_IN = 2_000, COST = 2_000;

    private final DockEnergy energy = new DockEnergy();

    /** The buffer: filled from outside, only drained by docked golems. */
    public final class DockEnergy extends EnergyStorage {
        DockEnergy() {
            super(CAPACITY, MAX_IN, 0);
        }

        void take(int n) {
            energy = Math.max(0, energy - n);
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int got = super.receiveEnergy(amount, simulate);
            if (got > 0 && !simulate) {
                setChanged();
            }
            return got;
        }
    }

    public GolemDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOLEM_DOCK.get(), pos, state);
    }

    public DockEnergy energy() {
        return energy;
    }

    /** Takes one fast-charge worth of FE if there is enough. */
    public boolean drawCharge() {
        if (energy.getEnergyStored() < COST) {
            return false;
        }
        energy.take(COST);
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", energy.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energy.deserializeNBT(registries, tag.get("Energy"));
        }
    }
}
