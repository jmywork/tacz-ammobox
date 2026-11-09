package com.taczammobox.mixin;

import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.taczammobox.config.FeatureFlags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses only the bolt animation state while an auto-fire magazine is mounted.
 *
 * <p>The bolt request itself must still run so the server chambers the next round; only
 * the hand-cycling animation is skipped, which is what made the gun look like it was
 * being cranked by hand. TACZ spells the state "blot".</p>
 */
@Mixin(AnimationStateMachine.class)
public abstract class AnimationStateMachineMixin {
    private static boolean taczammobox$warned;

    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczammobox$skipBoltAnimation(String stateName, CallbackInfo ci) {
        try {
            if (!FeatureFlags.autoFire() || !"blot".equals(stateName)) {
                return;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && FeatureFlags.isTierMagazine(player.getMainHandItem())) {
                ci.cancel();
            }
        } catch (Throwable throwable) {
            if (!taczammobox$warned) {
                taczammobox$warned = true;
                throwable.printStackTrace();
            }
        }
    }
}
