package com.taczammobox.mixin;

import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.taczammobox.util.AutoFireTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mutes the per-shot bolt sound while an auto-fire magazine is mounted; at 20 rounds per
 * second the cycling sound would otherwise stack into noise.
 */
@Mixin(SoundPlayManager.class)
public abstract class SoundPlayManagerMixin {
    @Inject(method = "playBoltSound", at = @At("HEAD"), cancellable = true, remap = false)
    private static void taczammobox$skipBoltSound(LivingEntity entity, GunDisplayInstance display, CallbackInfo ci) {
        if (!com.taczammobox.config.FeatureFlags.autoFire()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && AutoFireTier.of(player.getMainHandItem()) != null) {
            ci.cancel();
        }
    }
}
