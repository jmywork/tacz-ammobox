package com.taczammobox.api;

import com.tacz.guns.api.item.IAmmoBox;
import com.taczammobox.AmmoType;
import com.taczammobox.TaczAmmoBoxMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public registry for third-party capacity providers.
 *
 * <p>Resolution order: explicit config override, data-driven ammo capacity,
 * registered providers, DCSharp, automatic probe, TACZ formula.</p>
 */
public final class AmmoBoxCapacityApi {
    private static final Map<ResourceLocation, AmmoBoxCapacityProvider> PROVIDERS = new LinkedHashMap<>();

    private AmmoBoxCapacityApi() {
    }

    public static void register(ResourceLocation id, AmmoBoxCapacityProvider provider) {
        if (id == null || provider == null) {
            throw new IllegalArgumentException("id and provider must not be null");
        }
        PROVIDERS.put(id, provider);
        TaczAmmoBoxMod.LOGGER.info("Registered ammo box capacity provider {}", id);
    }

    public static void register(String id, AmmoBoxCapacityProvider provider) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid capacity provider id: " + id);
        }
        register(parsed, provider);
    }

    /** @return a positive capacity, or null when no provider handles the box. */
    public static Integer findCapacity(IAmmoBox box, ItemStack stack, AmmoType type, int level, int formula) {
        for (Map.Entry<ResourceLocation, AmmoBoxCapacityProvider> entry : PROVIDERS.entrySet()) {
            try {
                Integer value = entry.getValue().getCapacity(box, stack, type, level, formula);
                if (value != null && value > 0) {
                    return value;
                }
            } catch (RuntimeException exception) {
                TaczAmmoBoxMod.LOGGER.warn("Capacity provider {} failed for {}",
                        entry.getKey(), type.ammoId(), exception);
            }
        }
        return null;
    }
}
