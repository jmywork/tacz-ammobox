package com.taczammobox.compat.wthit;

import com.taczammobox.block.AmmoLoaderBlockEntity;
import com.taczammobox.block.AmmoPackerBlockEntity;
import com.taczammobox.compat.TooltipLines;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MachineProvider implements IBlockComponentProvider {
    public static final MachineProvider INSTANCE = new MachineProvider();

    private MachineProvider() {
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof AmmoPackerBlockEntity packer) {
            TooltipLines.packer(packer).forEach(tooltip::addLine);
        } else if (blockEntity instanceof AmmoLoaderBlockEntity loader) {
            TooltipLines.loader(loader).forEach(tooltip::addLine);
        }
    }
}
