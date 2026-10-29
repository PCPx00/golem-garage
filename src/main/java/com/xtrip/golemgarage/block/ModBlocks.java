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
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> s.getValue(LaserEmitterBlock.LIT) ? 13 : 6)));

    public static final DeferredBlock<SteelEmitterBlock> STEEL_EMITTER = BLOCKS.register(
            "steel_emitter",
            () -> new SteelEmitterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(4.0f, 8.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> s.getValue(LaserEmitterBlock.LIT) ? 13 : 6)));

    /** A roller door: counts as Garage wall, rolls up for golems, redstone or a right-click. */
    public static final DeferredBlock<GarageDoorBlock> GARAGE_DOOR = BLOCKS.register(
            "garage_door",
            () -> new GarageDoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .isValidSpawn((st, l, p, t) -> false).isSuffocating((st, l, p) -> false)
                    .isViewBlocking((st, l, p) -> false).isRedstoneConductor((st, l, p) -> false)));

    /** Steel-framed glass for Garage walls and roofs, so you can watch the repairs. */
    public static final DeferredBlock<net.minecraft.world.level.block.TransparentBlock> GARAGE_WINDOW = BLOCKS.register(
            "garage_window",
            () -> new net.minecraft.world.level.block.TransparentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
                    .strength(1.5f, 6.0f).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((st, l, p, t) -> false).isSuffocating((st, l, p) -> false)
                    .isViewBlocking((st, l, p) -> false).isRedstoneConductor((st, l, p) -> false)));

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
