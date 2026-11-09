package com.taczammobox.mixin;

import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.taczammobox.config.FeatureFlags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a gun reload even without a TACZ ammo box when the feed belt can supply rounds.
 *
 * <p>TACZ refuses to start a reload unless {@code canReload} says there is ammo. The belt
 * turns our boxed/crated ammo into that ammo source; the actual fill happens once the
 * reload finishes, so the animation and cooldown stay vanilla.</p>
 */
@Mixin(AbstractGunItem.class)
public abstract class AbstractGunItemReloadMixin {
    private static boolean taczammobox$warned;

    @Inject(method = "canReload", at = @At("RETURN"), cancellable = true, remap = false)
    private void taczammobox$chainCanReload(LivingEntity shooter, ItemStack gunStack,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            if (cir.getReturnValue() || !(shooter instanceof Player player)) {
                return;
            }
            if (FeatureFlags.chainHasReloadSource(player, gunStack)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable throwable) {
            if (!taczammobox$warned) {
                taczammobox$warned = true;
                throwable.printStackTrace();
            }
        }
    }
}
