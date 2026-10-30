package com.taczammobox.item;

import com.taczammobox.AmmoType;

/**
 * Common view over boxes and crates so the Create deployer compat can fill
 * ammo boxes without caring which package item is held.
 */
public interface PackagedAmmo {
    AmmoType getType();

    /** How many box-units this item represents (box = 1, crate = 9). */
    int boxesPerItem();
}
