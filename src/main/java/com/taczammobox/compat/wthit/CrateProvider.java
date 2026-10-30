package com.taczammobox.compat.wthit;

import com.taczammobox.block.DynamicCrateBlockEntity;
import com.taczammobox.compat.TooltipLines;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CrateProvider implements IBlockComponentProvider {
    public static final CrateProvider INSTANCE = new CrateProvider();

    private CrateProvider() {
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof DynamicCrateBlockEntity crate) {
            tooltip.addLine(TooltipLines.crate(crate));
        }
    }
}
