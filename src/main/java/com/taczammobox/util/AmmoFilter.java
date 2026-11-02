package com.taczammobox.util;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.taczammobox.item.PackagedAmmo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Reads the ammo type of a filter item and checks machine candidates against it. */
public final class AmmoFilter {
    private AmmoFilter() {
    }

    public static ResourceLocation ammoId(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof IAmmo ammo) {
            return ammo.getAmmoId(stack);
        }
        if (stack.getItem() instanceof PackagedAmmo pkg) {
            return pkg.getType().ammoId();
        }
        if (stack.getItem() instanceof IAmmoBox box) {
            ResourceLocation id = box.getAmmoId(stack);
            return DefaultAssets.EMPTY_AMMO_ID.equals(id) ? null : id;
        }
        return null;
    }

    public static boolean isValidFilter(ItemStack stack) {
        return !stack.isEmpty() && ammoId(stack) != null;
    }

    public static boolean matches(ItemStack filter, ItemStack candidate) {
        if (filter.isEmpty()) {
            return true;
        }
        ResourceLocation wanted = ammoId(filter);
        if (wanted == null) {
            return true;
        }
        return wanted.equals(ammoId(candidate));
    }

    /** Empty ammo boxes are allowed through a filter so they can be filled with the selected type. */
    public static boolean matchesOrEmptyBox(ItemStack filter, ItemStack candidate) {
        if (filter.isEmpty()) {
            return true;
        }
        if (candidate.getItem() instanceof IAmmoBox box
                && DefaultAssets.EMPTY_AMMO_ID.equals(box.getAmmoId(candidate))) {
            return true;
        }
        return matches(filter, candidate);
    }
}
