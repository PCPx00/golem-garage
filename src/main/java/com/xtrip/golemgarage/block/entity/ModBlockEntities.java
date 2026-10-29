package com.xtrip.golemgarage.block.entity;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GolemGarage.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GarageControllerBlockEntity>> GARAGE_CONTROLLER =
            BLOCK_ENTITIES.register("garage_controller",
                    () -> BlockEntityType.Builder.of(GarageControllerBlockEntity::new, ModBlocks.GARAGE_CONTROLLER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GarageDoorBlockEntity>> GARAGE_DOOR =
            BLOCK_ENTITIES.register("garage_door",
                    () -> BlockEntityType.Builder.of(GarageDoorBlockEntity::new, ModBlocks.GARAGE_DOOR.get())
                            .build(null));

    private ModBlockEntities() {
    }
}
