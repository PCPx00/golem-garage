package com.xtrip.golemgarage.block;

import com.xtrip.golemgarage.GolemGarage;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GolemGarage.MOD_ID);

    public static final DeferredBlock<GarageControllerBlock> GARAGE_CONTROLLER = BLOCKS.register(
            "garage_controller",
            () -> new GarageControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).lightLevel(s -> 4)));

    public static final DeferredBlock<LaserEmitterBlock> LASER_EMITTER = BLOCKS.register(
            "laser_emitter",
            () -> new LaserEmitterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 6)));

    public static final DeferredBlock<SteelEmitterBlock> STEEL_EMITTER = BLOCKS.register(
            "steel_emitter",
            () -> new SteelEmitterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(4.0f, 8.0f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 6)));

    public static final DeferredBlock<SignalPlateBlock> SIGNAL_PLATE = BLOCKS.register(
            "signal_plate",
            () -> new SignalPlateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(0.5f).sound(SoundType.METAL).noCollission().noOcclusion()));

    public static final DeferredBlock<GolemDockBlock> GOLEM_DOCK = BLOCKS.register(
            "golem_dock",
            () -> new GolemDockBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> s.getValue(GolemDockBlock.OCCUPIED) ? 9 : 3)));

    private ModBlocks() {
    }
}
