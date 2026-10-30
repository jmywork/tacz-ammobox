package com.taczammobox.compat;

import com.taczammobox.AmmoType;
import com.taczammobox.block.AmmoLoaderBlockEntity;
import com.taczammobox.block.AmmoPackerBlockEntity;
import com.taczammobox.block.DynamicCrateBlockEntity;
import com.taczammobox.registry.AmmoTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared text for the optional Jade and WTHIT overlays. */
public final class TooltipLines {
    private TooltipLines() {
    }

    public static List<Component> packer(AmmoPackerBlockEntity packer) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.taczammobox.mode",
                Component.translatable(packer.isUnpacking()
                        ? "tooltip.taczammobox.mode.unpack"
                        : "tooltip.taczammobox.mode.pack")));
        lines.add(redstone(packer.getRedstoneMode().name()));
        lines.add(progress(packer.getProgress(), AmmoPackerBlockEntity.ticksPerOperation()));
        return lines;
    }

    public static List<Component> loader(AmmoLoaderBlockEntity loader) {
        List<Component> lines = new ArrayList<>();
        lines.add(redstone(loader.getRedstoneMode().name()));
        lines.add(progress(loader.getProgress(), AmmoLoaderBlockEntity.ticksPerOperation()));
        return lines;
    }

    public static Component crate(DynamicCrateBlockEntity crate) {
        ResourceLocation id = crate.getAmmoId();
        AmmoType type = id == null ? null : AmmoTypes.byId(id);
        Component name = type != null ? type.displayName()
                : Component.literal(id == null ? "?" : id.toString());
        return Component.translatable("tooltip.taczammobox.ammo", name);
    }

    private static Component redstone(String name) {
        return Component.translatable("tooltip.taczammobox.redstone",
                Component.translatable("tooltip.taczammobox.redstone." + name.toLowerCase(Locale.ROOT)));
    }

    private static Component progress(int progress, int ticks) {
        return Component.translatable("tooltip.taczammobox.progress", progress, ticks);
    }
}
