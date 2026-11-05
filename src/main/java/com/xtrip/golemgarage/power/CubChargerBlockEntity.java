package com.xtrip.golemgarage.power;

import com.xtrip.golemgarage.entity.CubEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Cub Charger: a little powered pad. Every second it spends 200 FE on the Cubs within 5 blocks: they heal and
 * get a minute of the treat bonus (2 more blocks of magnet reach). It glows while it has power.
 */
public class CubChargerBlockEntity extends BlockEntity {
    public static final int COST = 200;
    private final PowerBuffer power = new PowerBuffer(50_000, 1_000, this::setChanged);

    public CubChargerBlockEntity(BlockPos pos, BlockState state) {
        super(PowerBlocks.CUB_CHARGER_BE.get(), pos, state);
    }

    public PowerBuffer power() {
        return power;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CubChargerBlockEntity c) {
        boolean on = c.power.getEnergyStored() >= COST;
        if (state.getValue(com.xtrip.golemgarage.block.CubChargerBlock.LIT) != on) {
            level.setBlock(pos, state.setValue(com.xtrip.golemgarage.block.CubChargerBlock.LIT, on), 3);
        }
        if (!on || (level.getGameTime() + pos.asLong()) % 20 != 0) {
            return;
        }
        var cubs = level.getEntitiesOfClass(CubEntity.class, new AABB(pos).inflate(5));
        if (cubs.isEmpty()) {
            return;
        }
        c.power.use(COST);
        for (CubEntity cub : cubs) {
            cub.charge();
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cub.getX(), cub.getY() + 0.6, cub.getZ(), 4, 0.25, 0.3, 0.25, 0.05);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        power.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power.load(tag);
    }
}
