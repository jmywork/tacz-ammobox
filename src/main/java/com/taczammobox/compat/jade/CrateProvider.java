package com.taczammobox.compat.jade;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.block.DynamicCrateBlockEntity;
import com.taczammobox.compat.TooltipLines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class CrateProvider implements IBlockComponentProvider {
    public static final CrateProvider INSTANCE = new CrateProvider();
    private static final ResourceLocation UID =
            new ResourceLocation(TaczAmmoBoxMod.MOD_ID, "crate");

    private CrateProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof DynamicCrateBlockEntity crate) {
            tooltip.add(TooltipLines.crate(crate));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
