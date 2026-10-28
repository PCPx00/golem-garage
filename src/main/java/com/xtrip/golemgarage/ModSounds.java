package com.xtrip.golemgarage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Golem Garage's own sounds (synthesized by tools/golemgen/sounds.py). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GolemGarage.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_STEP = reg("golem.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_STEP_WHEELS = reg("golem.step_wheels");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_STEP_TREADS = reg("golem.step_treads");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_HURT = reg("golem.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_ATTACK = reg("golem.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_BREAKDOWN = reg("golem.breakdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_WORK = reg("golem.work");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_AMBIENT = reg("golem.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_PART_FIT = reg("golem.part_fit");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_POWER_LOW = reg("golem.power_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_MODULE = reg("golem.module");
    public static final DeferredHolder<SoundEvent, SoundEvent> GARAGE_LASER = reg("garage.laser");
    public static final DeferredHolder<SoundEvent, SoundEvent> GARAGE_START = reg("garage.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> GARAGE_DONE = reg("garage.done");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUB_CHIRP = reg("cub.chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUB_HAPPY = reg("cub.happy");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUB_SLEEP = reg("cub.sleep");

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, name)));
    }

    private ModSounds() {
    }
}
