package com.xtrip.golemgarage.menu;

import com.xtrip.golemgarage.GolemGarage;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GolemGarage.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CubMenu>> CUB =
            MENUS.register("cub", () -> IMenuTypeExtension.create(CubMenu::fromNetwork));

    private ModMenus() {
    }
}
