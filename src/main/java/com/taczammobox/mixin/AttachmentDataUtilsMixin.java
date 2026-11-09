package com.taczammobox.mixin;

import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import com.taczammobox.config.FeatureFlags;
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
    private static boolean taczammobox$warned;

    @Inject(method = "getAmmoCountWithAttachment", at = @At("RETURN"), cancellable = true, remap = false)
    private static void taczammobox$autoSearCapacity(ItemStack gunStack, GunData gunData,
            CallbackInfoReturnable<Integer> cir) {
        try {
            if (!FeatureFlags.capacityOverride() || gunData.getBolt() != Bolt.MANUAL_ACTION) {
                return;
            }
            int capacity = FeatureFlags.tierCapacityFor(gunStack);
            if (capacity > 0) {
                com.taczammobox.util.AutoFireServer.DIAG.logChange("capacity", capacity + "r "
                        + FeatureFlags.magazineForLog(gunStack));
                cir.setReturnValue(capacity);
            }
        } catch (Throwable throwable) {
            if (!taczammobox$warned) {
                taczammobox$warned = true;
                throwable.printStackTrace();
            }
        }
    }
}
