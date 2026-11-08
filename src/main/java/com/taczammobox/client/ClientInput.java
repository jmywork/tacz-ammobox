package com.taczammobox.client;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.item.PackagedAmmo;
import com.taczammobox.network.ModNetwork;
import com.tacz.guns.api.item.IAmmo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
        if (ModKeyMappings.FOCUS_TARGET.consumeClick()) {
            ModNetwork.sendFocus(pickFocusTarget(minecraft));
        }
        if (ModKeyMappings.SORT_AMMO.consumeClick()) {
            TaczAmmoBoxMod.LOGGER.info("[key] pack pressed");
            ModNetwork.sendPack();
        }
        if (!(minecraft.player.getMainHandItem().getItem() instanceof PackagedAmmo)
                && !(minecraft.player.getMainHandItem().getItem() instanceof IAmmo)) {
            return;
        }
        if (ModKeyMappings.FILL_AMMO_BOX.consumeClick()) {
            TaczAmmoBoxMod.LOGGER.info("[key] fill pressed (hand={})",
                    BuiltInRegistries.ITEM.getKey(minecraft.player.getMainHandItem().getItem()));
            ModNetwork.sendFill();
        }
    }

    /** What the crosshair is on: vanilla's own pick first, then a manual ray. */
    private static int pickFocusTarget(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return 0;
        }
        if (minecraft.crosshairPickEntity != null) {
            return minecraft.crosshairPickEntity.getId();
        }
        var player = minecraft.player;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(64.0D));
        AABB box = player.getBoundingBox().expandTowards(look.scale(64.0D)).inflate(1.0D);
        var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, box,
                entity -> !entity.isSpectator() && entity.isPickable(), 64.0D * 64.0D);
        return hit == null ? 0 : hit.getEntity().getId();
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
        if (ModKeyMappings.SORT_AMMO.matches(event.getKey(), event.getScanCode())) {
            ModNetwork.sendPack();
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