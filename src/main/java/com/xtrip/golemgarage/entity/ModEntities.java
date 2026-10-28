package com.xtrip.golemgarage.entity;

import com.xtrip.golemgarage.GolemGarage;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, GolemGarage.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GolemEntity>> GOLEM = ENTITIES.register(
            "golem",
            () -> EntityType.Builder.of(GolemEntity::new, MobCategory.MISC)
                    .sized(1.4f, 2.7f)
                    .build("golem"));

    public static final DeferredHolder<EntityType<?>, EntityType<CubEntity>> CUB = ENTITIES.register(
            "cub",
            () -> EntityType.Builder.of(CubEntity::new, MobCategory.CREATURE)
                    .sized(0.7f, 1.1f)
                    .build("cub"));

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(GOLEM.get(), GolemEntity.createAttributes().build());
        event.put(CUB.get(), CubEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
