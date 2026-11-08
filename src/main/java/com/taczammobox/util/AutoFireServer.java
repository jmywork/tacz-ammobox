package com.taczammobox.util;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.taczammobox.TaczAmmoBoxMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps tier magazines ready to fire and accounts for their ammunition.
 *
 * <p>TACZ fires client-side (that is where the muzzle flash, recoil and gunshot sound are
 * played), so this mod leaves shooting to TACZ and only removes the bolt gate: the bolt
 * flag is cleared on both sides and the bolt script is skipped, which is what used to
 * cap every tier at the same bursty cadence.</p>
 *
 * <p>Because the bolt script no longer runs, the round it would have pulled from the
 * magazine is taken here, on the server's shot event: exactly one round per shot, no
 * free rounds and no double consumption.</p>
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID)
public final class AutoFireServer {
    private AutoFireServer() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        ItemStack gunStack = player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null || AutoFireTier.of(gunStack) == null) {
            return;
        }
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack)).orElse(null);
        if (index == null || index.getGunData().getBolt() != Bolt.MANUAL_ACTION) {
            return;
        }
        ShooterDataHolder holder = IGunOperator.fromLivingEntity(player).getDataHolder();
        boolean wasBolting = holder.isBolting;
        holder.isBolting = false;
        holder.boltTimestamp = 0;
        if (wasBolting) {
            DIAG.log("server-bolt", "cleared isBolting");
        }
        // Keep the chamber loaded so TACZ's NEED_BOLT check passes; an empty magazine must
        // block shooting though, otherwise the last round would never run out.
        boolean loaded = gun.hasBulletInBarrel(gunStack);
        int count = gun.getCurrentAmmoCount(gunStack);
        if (count <= 0 && loaded) {
            gun.setBulletInBarrel(gunStack, false);
        } else if (count > 0 && !loaded) {
            gun.setBulletInBarrel(gunStack, true);
        }
    }

    /** Rate-limited diagnostics for the tier magazine fire behaviour. */
    public static final class DIAG {
        private static final java.util.Map<String, Long> LAST = new java.util.HashMap<>();
        private static long lastShotLog = System.currentTimeMillis();
        private static int shotsSinceLog;
        private static long totalShots;

        private DIAG() {
        }

        public static void log(String tag, String message) {
            long now = System.currentTimeMillis();
            Long last = LAST.get(tag);
            if (last != null && now - last < 800L) {
                return;
            }
            LAST.put(tag, now);
            TaczAmmoBoxMod.LOGGER.info("[taczammobox][shoot][{}] {} (total shots={})", tag, message, totalShots);
        }

        public static void shot() {
            shotsSinceLog++;
            totalShots++;
            long now = System.currentTimeMillis();
            if (now - lastShotLog >= 1000L) {
                TaczAmmoBoxMod.LOGGER.info("[taczammobox][shoot][rate] server accepted {} shots in {} ms",
                        shotsSinceLog, now - lastShotLog);
                shotsSinceLog = 0;
                lastShotLog = now;
            }
        }
    }

    /** Takes the round the skipped bolt script would have chambered. */
    @SubscribeEvent
    public static void onGunShoot(GunShootEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) {
            return;
        }
        ItemStack gunStack = event.getGunItemStack();
        if (AutoFireTier.of(gunStack) == null) {
            return;
        }
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return;
        }
        int count = gun.getCurrentAmmoCount(gunStack);
        if (count > 0) {
            gun.setCurrentAmmoCount(gunStack, count - 1);
        }
        gun.setBulletInBarrel(gunStack, true);
        DIAG.shot();
    }
}
