package com.taczammobox.registry;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.menu.AmmoLoaderMenu;
import com.taczammobox.menu.AmmoPackerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, TaczAmmoBoxMod.MOD_ID);

    public static final RegistryObject<MenuType<AmmoPackerMenu>> PACKER =
            MENUS.register("ammo_packer", () -> IForgeMenuType.create(AmmoPackerMenu::new));

    public static final RegistryObject<MenuType<AmmoLoaderMenu>> LOADER =
            MENUS.register("ammo_loader", () -> IForgeMenuType.create(AmmoLoaderMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
