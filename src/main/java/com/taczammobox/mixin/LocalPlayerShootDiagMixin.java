package com.taczammobox.mixin;

import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import com.taczammobox.TaczAmmoBoxMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary diagnostic: counts why the client refused a shot, once per second.
 *
 * <p>Only used while chasing the tier magazine cadence; it never changes the result.</p>
 */
@Mixin(LocalPlayerShoot.class)
public abstract class LocalPlayerShootDiagMixin {
    private static final Map<String, Integer> TACZAMMOBOX$COUNTS = new HashMap<>();
    private static long taczammobox$lastReport = System.currentTimeMillis();

    @Inject(method = "shoot", at = @At("RETURN"), remap = false)
    private void taczammobox$logShootResult(CallbackInfoReturnable<ShootResult> cir) {
        try {
            ShootResult result = cir.getReturnValue();
            String name = result == null ? "NULL" : result.name();
            TACZAMMOBOX$COUNTS.merge(name, 1, Integer::sum);
            long now = System.currentTimeMillis();
            if (now - taczammobox$lastReport < 1000L) {
                return;
            }
            TaczAmmoBoxMod.LOGGER.info("[taczammobox][shoot][client] results in {} ms: {}",
                    now - taczammobox$lastReport, TACZAMMOBOX$COUNTS);
            TACZAMMOBOX$COUNTS.clear();
            taczammobox$lastReport = now;
        } catch (Throwable ignored) {
            // Diagnostics must never break the shot.
        }
    }
}
