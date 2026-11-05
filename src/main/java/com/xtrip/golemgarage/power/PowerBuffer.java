package com.xtrip.golemgarage.power;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * A machine's energy input: other mods' cables, Grim pipes and the mod's own generators and batteries push into it.
 * Machines spend it with {@link #use(int)} for a speed-up.
 */
public class PowerBuffer extends EnergyStorage {
    private final Runnable changed;

    public PowerBuffer(int capacity, int maxIn, Runnable changed) {
        super(capacity, maxIn, 0);
        this.changed = changed;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        int got = super.receiveEnergy(amount, simulate);
        if (got > 0 && !simulate) {
            changed.run();
        }
        return got;
    }

    /** Spends n FE if there is that much. */
    public boolean use(int n) {
        if (energy < n) {
            return false;
        }
        energy -= n;
        changed.run();
        return true;
    }

    public void save(CompoundTag tag) {
        tag.putInt("Energy", energy);
    }

    public void load(CompoundTag tag) {
        energy = Math.max(0, Math.min(capacity, tag.getInt("Energy")));
    }
}
