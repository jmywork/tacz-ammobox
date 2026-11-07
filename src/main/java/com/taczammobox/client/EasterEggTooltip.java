package com.taczammobox.client;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.config.FeatureFlags;
import com.taczammobox.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Marks the toy items as easter eggs in their tooltip.
 *
 * <p>The belt and the three auto-fire magazines are deliberately unbalanced, so they
 * live in the normal creative tab instead of a hidden one, but carry a visible label
 * rather than looking like regular gear.</p>
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID, value = Dist.CLIENT)
public final class EasterEggTooltip {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        boolean easterEgg = stack.is(ModItems.AMMO_BELT.get()) || FeatureFlags.isTierMagazine(stack);
        if (easterEgg) {
            event.getToolTip().add(Component.translatable("tooltip.taczammobox.easter_egg")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private EasterEggTooltip() {
    }
}
