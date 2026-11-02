package com.taczammobox.api;

import com.tacz.guns.api.item.IAmmoBox;
import com.taczammobox.AmmoType;
import net.minecraft.world.item.ItemStack;

/**
 * Optional integration point for addons that know the real capacity of a custom
 * TACZ ammo box. Register through {@link AmmoBoxCapacityApi}.
 */
@FunctionalInterface
public interface AmmoBoxCapacityProvider {
    /**
     * @return the capacity in rounds, or {@code null} to fall through to the next resolver
     */
    Integer getCapacity(IAmmoBox box, ItemStack stack, AmmoType type, int level, int formula);
}
