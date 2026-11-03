package com.taczammobox.util;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.taczammobox.TaczAmmoBoxMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The three auto-fire magazine tiers. Each tier is a separate attachment id so the
 * player can pick one in the refit screen; capacity and RPM are enforced in code
 * instead of the gun's extended-mag level table.
 */
public enum AutoFireTier {
    TIER_1("auto_sear", 1, 30, 400),
    TIER_2("auto_sear_2", 2, 45, 600),
    TIER_3("auto_sear_3", 3, 60, 1200);

    private final String path;
    private final ResourceLocation id;
    private final int level;
    private final int capacity;
    private final int rpm;

    AutoFireTier(String path, int level, int capacity, int rpm) {
        this.path = path;
        this.id = new ResourceLocation(TaczAmmoBoxMod.MOD_ID, path);
        this.level = level;
        this.capacity = capacity;
        this.rpm = rpm;
    }

    public ResourceLocation id() {
        return id;
    }

    public String path() {
        return path;
    }

    /** Extended magazine level, used for the tooltip and the refit diagram. */
    public int level() {
        return level;
    }

    public int capacity() {
        return capacity;
    }

    public int rpm() {
        return rpm;
    }

    /** Shoot interval in milliseconds. */
    public long shootInterval() {
        return 60000L / rpm;
    }

    public static AutoFireTier of(ResourceLocation attachmentId) {
        if (attachmentId == null) {
            return null;
        }
        for (AutoFireTier tier : values()) {
            if (tier.id.equals(attachmentId)) {
                return tier;
            }
        }
        return null;
    }

    /** Tier installed on a gun stack, or null when none of ours is mounted. */
    public static AutoFireTier of(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return null;
        }
        return of(gun.getAttachmentId(gunStack, AttachmentType.EXTENDED_MAG));
    }
}
