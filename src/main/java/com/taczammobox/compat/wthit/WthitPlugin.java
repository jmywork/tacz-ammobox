package com.taczammobox.compat.wthit;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.block.AmmoLoaderBlock;
import com.taczammobox.block.AmmoPackerBlock;
import com.taczammobox.block.DynamicCrateBlock;
import mcp.mobius.waila.api.IPluginInfo;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;

@WailaPlugin(id = TaczAmmoBoxMod.MOD_ID, side = IPluginInfo.Side.CLIENT)
public class WthitPlugin implements IWailaPlugin {
    @Override
    public void register(IRegistrar registrar) {
        registrar.addComponent(MachineProvider.INSTANCE, TooltipPosition.BODY, AmmoPackerBlock.class);
        registrar.addComponent(MachineProvider.INSTANCE, TooltipPosition.BODY, AmmoLoaderBlock.class);
        registrar.addComponent(CrateProvider.INSTANCE, TooltipPosition.BODY, DynamicCrateBlock.class);
    }
}
