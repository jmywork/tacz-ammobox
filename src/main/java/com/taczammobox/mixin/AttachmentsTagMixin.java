package com.taczammobox.mixin;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.manager.AttachmentsTagManager;
import com.taczammobox.config.FeatureFlags;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/**
 * TACZ decides attachment compatibility with per-gun tag files
 * ({@code tacz_tags/attachments/allow_attachments/<gun>.json}), so a new attachment
 * from another gun pack is rejected by every gun. This makes every bolt-action gun
 * accept all auto-fire magazines without touching the gun packs themselves.
 */
@Mixin(AttachmentsTagManager.class)
public abstract class AttachmentsTagMixin {
    private static boolean taczammobox$warned;

    @Inject(method = "getAllowAttachmentTags", at = @At("RETURN"), cancellable = true, remap = false)
    private void taczammobox$allowAutoSear(ResourceLocation gunId, CallbackInfoReturnable<Set<String>> cir) {
        try {
            if (!FeatureFlags.autoFire()) {
                return;
            }
            CommonGunIndex index = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
            if (index == null || index.getGunData().getBolt() != Bolt.MANUAL_ACTION) {
                return;
            }
            Set<String> original = cir.getReturnValue();
            Set<String> copy = original == null ? new HashSet<>() : new HashSet<>(original);
            copy.addAll(FeatureFlags.tierAttachmentIds());
            cir.setReturnValue(copy);
        } catch (Throwable throwable) {
            if (!taczammobox$warned) {
                taczammobox$warned = true;
                throwable.printStackTrace();
            }
        }
    }
}
