package com.taczammobox.mixin;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import com.taczammobox.config.FeatureFlags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the client's chamber flag in step with the server for tier magazines.
 *
 * <p>The bolt script never runs for these magazines, so the server keeps a round in the
 * barrel. The client copy of that flag is only refreshed by item sync, and a sync landing
 * between two shots makes TACZ's gate refuse every other shot as {@code NEED_BOLT}.
 * Correcting the flag at the head of the shot call guarantees the gate sees the same
 * state the server has.</p>
 */
@Mixin(LocalPlayerShoot.class)
public abstract class LocalPlayerShootChamberMixin {
    @Inject(method = "shoot", at = @At("HEAD"), remap = false)
    private void taczammobox$keepChamberLoaded(CallbackInfo ci) {
        try {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (!FeatureFlags.isTierMagazine(stack)) {
                return;
            }
            IGun gun = IGun.getIGunOrNull(stack);
            if (gun == null) {
                return;
            }
            int count = gun.getCurrentAmmoCount(stack);
            boolean chambered = gun.hasBulletInBarrel(stack);
            if (count > 0 && !chambered) {
                gun.setBulletInBarrel(stack, true);
            } else if (count <= 0 && chambered) {
                gun.setBulletInBarrel(stack, false);
            }
        } catch (Throwable ignored) {
            // Never keep a shot from happening because of this correction.
        }
    }
}
