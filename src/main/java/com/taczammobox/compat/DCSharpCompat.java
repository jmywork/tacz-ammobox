package com.taczammobox.compat;

import com.taczammobox.TaczAmmoBoxMod;
import net.minecraftforge.fml.ModList;

/**
 * Optional DCSharp compatibility. DCSharp's TACZ mixin overrides the ammo box
 * capacity with "ammo stack size * configured stack count" for iron/gold/diamond,
 * which is not TACZ's normal level formula and cannot be found by probing
 * setAmmoCount. Read its config reflectively so no hard dependency is needed.
 */
public final class DCSharpCompat {
    private static int[] stackCounts;
    private static boolean loaded;

    private DCSharpCompat() {
    }

    /** @return configured stack count for level 0/1/2, or -1 when unavailable. */
    public static int stackCountForLevel(int level) {
        if (level < 0 || level > 2 || !ModList.get().isLoaded("dcsharp")) {
            return -1;
        }
        if (!loaded) {
            load();
        }
        return stackCounts == null ? -1 : stackCounts[level];
    }

    public static void clearCache() {
        stackCounts = null;
        loaded = false;
    }

    private static void load() {
        loaded = true;
        try {
            Class<?> configClass = Class.forName("com.bawnorton.dcsharp.config.DCSharpConfig");
            Object config = configClass.getMethod("get").invoke(null);
            Object gameplay = configClass.getField("gameplay").get(config);
            Object tacz = gameplay.getClass().getField("tacz").get(gameplay);
            int iron = tacz.getClass().getField("ironAmmoBoxStackCount").getInt(tacz);
            int gold = tacz.getClass().getField("goldAmmoBoxStackCount").getInt(tacz);
            int diamond = tacz.getClass().getField("diamondAmmoBoxStackCount").getInt(tacz);
            if (iron > 0 || gold > 0 || diamond > 0) {
                stackCounts = new int[]{iron, gold, diamond};
                TaczAmmoBoxMod.LOGGER.info(
                        "Using DCSharp ammo box stack counts: iron={}, gold={}, diamond={}",
                        iron, gold, diamond);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            TaczAmmoBoxMod.LOGGER.warn("Could not read DCSharp ammo box stack counts", exception);
        }
    }
}
