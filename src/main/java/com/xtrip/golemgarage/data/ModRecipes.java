package com.xtrip.golemgarage.data;

import com.xtrip.golemgarage.GolemGarage;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, GolemGarage.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DatapackPartRecipe>> DATAPACK_PART =
            SERIALIZERS.register("datapack_part", () -> DatapackPartRecipe.SERIALIZER);

    private ModRecipes() {
    }
}
