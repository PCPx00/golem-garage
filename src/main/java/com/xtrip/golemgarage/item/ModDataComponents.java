package com.xtrip.golemgarage.item;

import com.mojang.serialization.Codec;
import com.xtrip.golemgarage.GolemGarage;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data: a part's material and copper state, and a Cub's color. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(GolemGarage.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MATERIAL =
            COMPONENTS.registerComponentType("material",
                    b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> OXIDATION =
            COMPONENTS.registerComponentType("oxidation",
                    b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> WAXED =
            COMPONENTS.registerComponentType("waxed",
                    b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COLOR =
            COMPONENTS.registerComponentType("color",
                    b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {
    }
}
