package com.taczammobox.compat.wthit;

import com.taczammobox.block.AmmoBoxPileBlockEntity;
import com.taczammobox.compat.TooltipLines;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PileProvider implements IBlockComponentProvider {
    public static final PileProvider INSTANCE = new PileProvider();

    private PileProvider() {
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof AmmoBoxPileBlockEntity pile) {
            for (var line : TooltipLines.pile(pile)) {
                tooltip.addLine(line);
            }
        }
    }
}
