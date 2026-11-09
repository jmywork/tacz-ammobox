package com.taczammobox.util;

import com.tacz.guns.api.item.IAmmo;
import com.taczammobox.AmmoType;
import com.taczammobox.item.PackagedAmmo;
import com.taczammobox.item.SatelliteRackItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Feeds upkeep racks from real ammo.
 *
 * <p>A rack keeps rounds per ammo id, exactly like a belt cache. Pulling is
 * three-staged so nothing is wasted: the rack's own cache first, then one of our
 * boxes/crates (converted to rounds, the remainder stays cached), then loose TACZ
 * ammo items (one item is one round).</p>
 */
public final class SatelliteAmmo {
    private SatelliteAmmo() {
    }

    public static int cached(ItemStack rack, ResourceLocation ammoId) {
        return SatelliteRackItem.cachedRounds(rack, ammoId);
    }

    /** Takes up to {@code want} rounds; returns how many were actually pulled. */
    public static int draw(Player player, ItemStack rack, ResourceLocation ammoId, int want) {
        if (want <= 0) {
            return 0;
        }
        int taken = 0;
        int fromCache = Math.min(want, SatelliteRackItem.cachedRounds(rack, ammoId));
        if (fromCache > 0) {
            SatelliteRackItem.addCachedRounds(rack, ammoId, -fromCache);
            taken += fromCache;
            want -= fromCache;
        }
        if (want <= 0) {
            return taken;
        }
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && want > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof PackagedAmmo packaged) {
                AmmoType type = packaged.getType();
                if (!type.ammoId().equals(ammoId)) {
                    continue;
                }
                int rounds = AmmoBoxHelper.roundsPerBox(type) * packaged.boxesPerItem();
                int used = Math.min(want, rounds);
                stack.shrink(1);
                if (rounds > used) {
                    SatelliteRackItem.addCachedRounds(rack, ammoId, rounds - used);
                }
                taken += used;
                want -= used;
                continue;
            }
            IAmmo ammo = IAmmo.getIAmmoOrNull(stack);
            if (ammo == null || !ammo.getAmmoId(stack).equals(ammoId)) {
                continue;
            }
            int used = Math.min(want, stack.getCount());
            stack.shrink(used);
            taken += used;
            want -= used;
        }
        return taken;
    }

    /** Refunds rounds that were drawn but never fired. */
    public static void refund(ItemStack rack, ResourceLocation ammoId, int rounds) {
        if (rounds > 0) {
            SatelliteRackItem.addCachedRounds(rack, ammoId, rounds);
        }
    }
}
