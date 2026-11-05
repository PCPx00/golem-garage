package com.xtrip.golemgarage.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * A block that makes Forge Energy. It keeps a buffer, pushes power into anything next to it that takes FE (pipes,
 * cables, machines, Golem Docks), and lets pipes pull from it on every side.
 */
public abstract class GeneratorBlockEntity extends BlockEntity {
    private final Buffer energy;
    private final int maxOut;
    /** FE made last tick (synced to the client for the screen, Jade and animations). */
    protected int rate;
    private int sentRate = -1, sentEnergy = -1;

    /** Extract-only from outside; filled by the generator itself. */
    public final class Buffer extends EnergyStorage {
        Buffer(int capacity, int maxOut) {
            super(capacity, 0, maxOut);
        }

        void add(int n) {
            energy = Math.min(capacity, energy + n);
        }

        void take(int n) {
            energy = Math.max(0, energy - n);
        }

        void set(int n) {
            energy = Math.max(0, Math.min(capacity, n));
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

    protected GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxOut) {
        super(type, pos, state);
        this.energy = new Buffer(capacity, maxOut);
        this.maxOut = maxOut;
    }

    public IEnergyStorage energy() {
        return energy;
    }

    public int stored() {
        return energy.getEnergyStored();
    }

    public int capacity() {
        return energy.getMaxEnergyStored();
    }

    public int rate() {
        return rate;
    }

    protected boolean full() {
        return energy.getEnergyStored() >= energy.getMaxEnergyStored();
    }

    protected void setStored(int n) {
        energy.set(n);
    }

    /** FE to make this tick. */
    protected abstract int produce(Level level);

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeneratorBlockEntity gen) {
        int made = gen.produce(level);
        gen.rate = made;
        if (made > 0) {
            gen.energy.add(made);
            gen.setChanged();
        }
        gen.push(level);
        // Keep clients roughly in step (for the screen, Jade and the turbine's spin).
        if (level.getGameTime() % 10 == (pos.asLong() & 7)
                && (gen.rate != gen.sentRate || Math.abs(gen.stored() - gen.sentEnergy) > gen.capacity() / 100)) {
            gen.sentRate = gen.rate;
            gen.sentEnergy = gen.stored();
            level.sendBlockUpdated(pos, state, state, 2);
        }
    }

    private void push(Level level) {
        int left = Math.min(maxOut, energy.getEnergyStored());
        if (left <= 0) {
            return;
        }
        for (Direction d : Direction.values()) {
            IEnergyStorage to = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(d), d.getOpposite());
            if (to == null || !to.canReceive() || to == energy) {
                continue;
            }
            int sent = to.receiveEnergy(left, false);
            if (sent > 0) {
                energy.take(sent);
                left -= sent;
                setChanged();
                if (left <= 0) {
                    return;
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Rate", rate);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.set(tag.getInt("Energy"));
        rate = tag.getInt("Rate");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
