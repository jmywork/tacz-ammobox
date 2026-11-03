package com.taczammobox.mixin;

import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.item.ModernKineticGunItem;
import com.taczammobox.util.AutoFireTier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Skips TACZ's bolt script for tier magazines, but only for real players.
 *
 * <p>The player path removes the bolt gate that capped the fire rate into bursts. Other
 * shooters must keep the vanilla bolt behaviour: the sentry arm fires through a
 * {@link FakePlayer} and waits for the bolt to finish after every manual-action shot, so
 * cancelling it there leaves the sentry stuck in its "bolting" state after one shot.</p>
 */
@Mixin(ModernKineticGunItem.class)
public abstract class ModernKineticGunBoltMixin {
    @Inject(method = "startBolt", at = @At("HEAD"), cancellable = true)
    private void taczammobox$skipBoltScript(ShooterDataHolder holder, ItemStack gunStack, LivingEntity shooter,
            CallbackInfoReturnable<Boolean> cir) {
        if (AutoFireTier.of(gunStack) == null || shooter instanceof FakePlayer) {
            return;
        }
        holder.isBolting = false;
        holder.boltTimestamp = 0;
        cir.setReturnValue(false);
    }
}
