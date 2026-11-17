package com.taczammobox.compat.jade;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.block.AmmoBoxPileBlockEntity;
import com.taczammobox.compat.TooltipLines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Shows what is inside a box pile: ammo type, box count and the round total. */
public final class PileProvider implements IBlockComponentProvider {
    public static final PileProvider INSTANCE = new PileProvider();
    private static final ResourceLocation UID = new ResourceLocation(TaczAmmoBoxMod.MOD_ID, "pile");

    private PileProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof AmmoBoxPileBlockEntity pile) {
            tooltip.addAll(TooltipLines.pile(pile));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
