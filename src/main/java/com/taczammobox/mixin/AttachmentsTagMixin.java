package com.taczammobox.mixin;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.manager.AttachmentsTagManager;
import com.taczammobox.util.AutoFireTier;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/**
 * TACZ 1.1.8 decides attachment compatibility with per-gun tag files
 * ({@code tacz_tags/attachments/allow_attachments/<gun>.json}), so a new attachment
 * from another gun pack is rejected by every gun. This makes every bolt-action gun
 * accept all {@link AutoFireTier} magazines without touching the gun packs themselves.
 */
@Mixin(AttachmentsTagManager.class)
public abstract class AttachmentsTagMixin {
    @Inject(method = "getAllowAttachmentTags", at = @At("RETURN"), cancellable = true)
    private void taczammobox$allowAutoSear(ResourceLocation gunId, CallbackInfoReturnable<Set<String>> cir) {
        if (!com.taczammobox.config.FeatureFlags.autoFire()) {
            return;
        }
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
        if (index == null || index.getGunData().getBolt() != Bolt.MANUAL_ACTION) {
            return;
        }
        Set<String> original = cir.getReturnValue();
        Set<String> copy = original == null ? new HashSet<>() : new HashSet<>(original);
        for (AutoFireTier tier : AutoFireTier.values()) {
            copy.add(tier.id().toString());
        }
        cir.setReturnValue(copy);
    }
}
