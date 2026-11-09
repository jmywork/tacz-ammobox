package com.taczammobox.compat;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.config.FeatureFlags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Startup check that the TACZ API this mod hooks into is actually present.
 *
 * <p>If a TACZ update moves a method or field, the mixins would either fail to apply or
 * throw at runtime. This check runs before the first world tick, reports what is missing
 * and turns the auto-fire feature off, so the mod degrades to "vanilla TACZ + our items"
 * instead of half-working.</p>
 */
public final class CompatSelfCheck {
    private CompatSelfCheck() {
    }

    public static void run() {
        List<String> missing = new ArrayList<>();

        requireClass(missing, "com.tacz.guns.api.item.IGun");
        requireClass(missing, "com.tacz.guns.util.AttachmentDataUtils");
        requireClass(missing, "com.tacz.guns.resource.manager.AttachmentsTagManager");
        requireClass(missing, "com.tacz.guns.item.ModernKineticGunItem");
        requireClass(missing, "com.tacz.guns.entity.shooter.ShooterDataHolder");

        requireMethod(missing, "com.tacz.guns.api.item.IGun", "getAttachmentId", ItemStack.class, AttachmentType.class);
        requireMethod(missing, "com.tacz.guns.util.AttachmentDataUtils", "getAmmoCountWithAttachment",
                ItemStack.class, GunData.class);
        requireMethod(missing, "com.tacz.guns.resource.pojo.data.gun.GunData", "getShootInterval",
                LivingEntity.class, com.tacz.guns.api.item.gun.FireMode.class, ItemStack.class);
        requireMethod(missing, "com.tacz.guns.item.ModernKineticGunItem", "startBolt",
                ShooterDataHolder.class, ItemStack.class, LivingEntity.class);
        requireField(missing, "com.tacz.guns.entity.shooter.ShooterDataHolder", "isBolting");
        requireField(missing, "com.tacz.guns.entity.shooter.ShooterDataHolder", "boltTimestamp");

        if (missing.isEmpty()) {
            TaczAmmoBoxMod.LOGGER.info("[taczammobox] TACZ compatibility check passed");
            return;
        }
        FeatureFlags.disableAutoFire("TACZ API changed: " + String.join(", ", missing));
        TaczAmmoBoxMod.LOGGER.warn("[taczammobox] auto-fire disabled, missing TACZ API: {}", missing);
    }

    private static void requireClass(List<String> missing, String name) {
        try {
            Class.forName(name, false, CompatSelfCheck.class.getClassLoader());
        } catch (Throwable throwable) {
            missing.add(name);
        }
    }

    private static void requireMethod(List<String> missing, String owner, String name, Class<?>... params) {
        try {
            Method method = Class.forName(owner, false, CompatSelfCheck.class.getClassLoader())
                    .getDeclaredMethod(name, params);
            method.setAccessible(true);
        } catch (Throwable throwable) {
            missing.add(owner + "#" + name);
        }
    }

    private static void requireField(List<String> missing, String owner, String name) {
        try {
            Field field = Class.forName(owner, false, CompatSelfCheck.class.getClassLoader())
                    .getDeclaredField(name);
            field.setAccessible(true);
        } catch (Throwable throwable) {
            missing.add(owner + "#" + name);
        }
    }
}
