package com.taczammobox;

import com.taczammobox.config.WasteSettings;
import com.taczammobox.network.ModNetwork;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.registry.ModBlockEntities;
import com.taczammobox.registry.ModBlocks;
import com.taczammobox.registry.ModCreativeTabs;
import com.taczammobox.registry.ModItems;
import com.taczammobox.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TaczAmmoBoxMod.MOD_ID)
public class TaczAmmoBoxMod {
    public static final String MOD_ID = "taczammobox";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TaczAmmoBoxMod(FMLJavaModLoadingContext context) {
        // Must run before ModItems is loaded: item ids have to exist at registry time.
        AmmoTypes.init();
        WasteSettings.load();

        IEventBus modBus = context.getModEventBus();
        ModNetwork.register();
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModCreativeTabs.register(modBus);
    }
}
