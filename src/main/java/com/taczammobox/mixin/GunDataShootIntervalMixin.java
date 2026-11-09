package com.taczammobox.mixin;

import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.taczammobox.config.FeatureFlags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the gun's RPM with the tier's RPM while an auto-fire magazine is mounted.
 *
 * <p>{@code getShootInterval} is the single place TACZ turns RPM into a cooldown
 * ({@code 60000 / rpm} milliseconds), so overriding it also drives the HUD cooldown and
 * the client-side send gate.</p>
 *
 * <p>The hook body is merged into {@code GunData}, so it may only talk to
 * {@link FeatureFlags}; see the bridge note there. Anything thrown - including a class
 * lookup refused by TACZ's module loader - disables just this override.</p>
 */
@Mixin(GunData.class)
public abstract class GunDataShootIntervalMixin {
    private static boolean taczammobox$warned;

    @Inject(method = "getShootInterval", at = @At("RETURN"), cancellable = true, remap = false)
    private void taczammobox$tierShootInterval(LivingEntity shooter, FireMode fireMode, ItemStack gunStack,
            CallbackInfoReturnable<Long> cir) {
        try {
            if (!FeatureFlags.rpmOverride()) {
                return;
            }
            GunData data = (GunData) (Object) this;
            if (data.getBolt() != Bolt.MANUAL_ACTION) {
                return;
            }
            long interval = FeatureFlags.tierIntervalFor(gunStack);
            if (interval > 0L) {
                cir.setReturnValue(interval);
            }
        } catch (Throwable throwable) {
            if (!taczammobox$warned) {
                taczammobox$warned = true;
                throwable.printStackTrace();
            }
        }
    }
}
