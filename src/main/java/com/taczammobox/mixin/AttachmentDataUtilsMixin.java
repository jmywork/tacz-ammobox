package com.taczammobox.mixin;

import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import com.taczammobox.util.AutoFireTier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives each auto-fire magazine tier its fixed capacity on bolt-action guns.
 *
 * <p>TACZ normally derives the capacity from the gun's {@code extended_mag_ammo_amount}
 * level table, which tops out at 10 for most snipers. Overriding the single method every
 * capacity consumer uses (HUD, tooltip, reload) keeps the numbers consistent.</p>
 */
@Mixin(AttachmentDataUtils.class)
public abstract class AttachmentDataUtilsMixin {
    @Inject(method = "getAmmoCountWithAttachment", at = @At("RETURN"), cancellable = true, remap = false)
    private static void taczammobox$autoSearCapacity(ItemStack gunStack, GunData gunData,
            CallbackInfoReturnable<Integer> cir) {
        if (!com.taczammobox.config.FeatureFlags.capacityOverride()
                || gunData.getBolt() != Bolt.MANUAL_ACTION) {
            return;
        }
        Integer capacity = com.taczammobox.util.FeatureGuard.get("capacityOverride", () -> {
            AutoFireTier tier = AutoFireTier.of(gunStack);
            return tier == null ? null : tier.capacity();
        }, null);
        if (capacity != null) {
            cir.setReturnValue(capacity);
        }
    }
}
