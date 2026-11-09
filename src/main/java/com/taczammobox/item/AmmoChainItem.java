package com.taczammobox.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Feed belt: while carried, a TACZ gun reloads straight from boxed/crated ammo instead of
 * requiring a TACZ ammo box. The reload animation and cooldown still play; the belt only
 * replaces the ammo source and caches leftovers in its own NBT.
 */
public class AmmoChainItem extends Item {
    public AmmoChainItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.taczammobox.ammo_chain.desc")
                .withStyle(ChatFormatting.GRAY));
        int rounds = AmmoBeltItem.storedRounds(stack);
        if (rounds > 0) {
            tooltip.add(Component.translatable("item.taczammobox.ammo_chain.buffer",
                    AmmoBeltItem.storedAmmoId(stack), rounds).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("item.taczammobox.ammo_chain.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
