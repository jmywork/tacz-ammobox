package com.taczammobox.mixin;

import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import com.taczammobox.util.AutoFireServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Temporary diagnostic: records why the client refused a shot.
 *
 * <p>Only used while chasing the tier magazine cadence; it never changes the result.</p>
 */
@Mixin(LocalPlayerShoot.class)
public abstract class LocalPlayerShootDiagMixin {
    @Inject(method = "shoot", at = @At("RETURN"), remap = false)
    private void taczammobox$logShootResult(CallbackInfoReturnable<ShootResult> cir) {
        try {
            ShootResult result = cir.getReturnValue();
            String name = result == null ? "NULL" : result.name();
            AutoFireServer.DIAG.log("client-" + name, name);
        } catch (Throwable ignored) {
            // Diagnostics must never break the shot.
        }
    }
}
