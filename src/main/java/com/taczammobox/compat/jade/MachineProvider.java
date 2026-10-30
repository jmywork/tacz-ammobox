package com.taczammobox.compat.jade;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.block.AmmoLoaderBlockEntity;
import com.taczammobox.block.AmmoPackerBlockEntity;
import com.taczammobox.compat.TooltipLines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class MachineProvider implements IBlockComponentProvider {
    public static final MachineProvider INSTANCE = new MachineProvider();
    private static final ResourceLocation UID =
            new ResourceLocation(TaczAmmoBoxMod.MOD_ID, "machine");

    private MachineProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof AmmoPackerBlockEntity packer) {
            tooltip.addAll(TooltipLines.packer(packer));
        } else if (blockEntity instanceof AmmoLoaderBlockEntity loader) {
            tooltip.addAll(TooltipLines.loader(loader));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
