package com.taczammobox.compat.jade;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.block.AmmoLoaderBlock;
import com.taczammobox.block.AmmoPackerBlock;
import com.taczammobox.block.DynamicCrateBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(TaczAmmoBoxMod.MOD_ID)
public class JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineProvider.INSTANCE, AmmoPackerBlock.class);
        registration.registerBlockComponent(MachineProvider.INSTANCE, AmmoLoaderBlock.class);
        registration.registerBlockComponent(CrateProvider.INSTANCE, DynamicCrateBlock.class);
    }
}
