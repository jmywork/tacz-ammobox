package com.taczammobox.mixin;

import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.taczammobox.util.AutoFireTier;
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
 */
@Mixin(GunData.class)
public abstract class GunDataShootIntervalMixin {
    @Inject(method = "getShootInterval", at = @At("RETURN"), cancellable = true, remap = false)
    private void taczammobox$tierShootInterval(LivingEntity shooter, FireMode fireMode, ItemStack gunStack,
            CallbackInfoReturnable<Long> cir) {
        if (!com.taczammobox.config.FeatureFlags.rpmOverride()) {
            return;
        }
        GunData data = (GunData) (Object) this;
        if (data.getBolt() != Bolt.MANUAL_ACTION) {
            return;
        }
        Long interval = com.taczammobox.util.FeatureGuard.get("rpmOverride", () -> {
            AutoFireTier tier = AutoFireTier.of(gunStack);
            return tier == null ? null : tier.shootInterval();
        }, null);
        if (interval != null) {
            cir.setReturnValue(interval);
        }
    }
}
