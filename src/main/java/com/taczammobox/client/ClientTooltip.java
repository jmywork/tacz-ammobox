package com.taczammobox.client;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.item.PackagedAmmo;
import com.tacz.guns.api.item.IAmmo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID, value = Dist.CLIENT)
public final class ClientTooltip {
    private ClientTooltip() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof PackagedAmmo
                || event.getItemStack().getItem() instanceof IAmmo) {
            event.getToolTip().add(Component.translatable("tooltip.taczammobox.fill",
                    ModKeyMappings.FILL_AMMO_BOX.getTranslatedKeyMessage()).withStyle(ChatFormatting.GRAY));
        }
    }
}