package com.taczammobox.client;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.item.PackagedAmmo;
import com.taczammobox.network.ModNetwork;
import com.tacz.guns.api.item.IAmmo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID, value = Dist.CLIENT)
public final class ClientInput {
    private ClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (!(minecraft.player.getMainHandItem().getItem() instanceof PackagedAmmo)
                && !(minecraft.player.getMainHandItem().getItem() instanceof IAmmo)) {
            return;
        }
        if (ModKeyMappings.FILL_AMMO_BOX.consumeClick()) {
            ModNetwork.sendFill();
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != 1) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.screen instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        if (screen.getFocused() instanceof EditBox) {
            return;
        }
        if (!ModKeyMappings.FILL_AMMO_BOX.matches(event.getKey(), event.getScanCode())) {
            return;
        }
        Slot hovered = screen.getSlotUnderMouse();
        if (hovered == null) {
            return;
        }
        ItemStack stack = hovered.getItem();
        if (!(stack.getItem() instanceof PackagedAmmo) && !(stack.getItem() instanceof IAmmo)) {
            return;
        }
        ModNetwork.sendFill(hovered.index, screen.getMenu().containerId);
    }
}