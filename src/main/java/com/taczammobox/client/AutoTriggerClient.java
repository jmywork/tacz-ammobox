package com.taczammobox.client;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.input.ShootKey;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.util.AutoFireTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client half of the tier magazines.
 *
 * <p>TACZ keeps its own fire loop so muzzle flash, recoil and gunshot sound stay intact.
 * The bolt gate lives in the client too: while the local bolt flag is set the client
 * holds shots back and then releases them as a burst, so it is cleared here every tick.
 * The hand animation is suppressed separately in {@code AnimationStateMachineMixin}.</p>
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID, value = Dist.CLIENT)
public final class AutoTriggerClient {
    private static int taczammobox$ticks;
    private static long taczammobox$lastTickReport = System.currentTimeMillis();
    private static boolean taczammobox$lastTrigger;

    private AutoTriggerClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        taczammobox$ticks++;
        long now = System.currentTimeMillis();
        if (now - taczammobox$lastTickReport >= 1000L) {
            com.taczammobox.util.AutoFireServer.DIAG.logChange("ticks",
                    "ticks/s=" + taczammobox$ticks + " fps=" + Minecraft.getInstance().getFps());
            taczammobox$ticks = 0;
            taczammobox$lastTickReport = now;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack gunStack = player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null || AutoFireTier.of(gunStack) == null) {
            return;
        }
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack)).orElse(null);
        if (index == null || index.getGunData().getBolt() != Bolt.MANUAL_ACTION) {
            return;
        }
        if (gun.getFireMode(gunStack) != FireMode.AUTO) {
            gun.setFireMode(gunStack, FireMode.AUTO);
        }
        boolean trigger = ShootKey.SHOOT_KEY.isDown();
        if (trigger != taczammobox$lastTrigger) {
            taczammobox$lastTrigger = trigger;
            com.taczammobox.util.AutoFireServer.DIAG.logNow("trigger",
                    (trigger ? "down" : "up") + " ammo=" + gun.getCurrentAmmoCount(gunStack));
        }
        // The server keeps the chamber loaded because the bolt script never runs. The
        // client's own copy of that flag is what TACZ's shoot gate reads, though, and it
        // is only refreshed by item sync, so mirror the server here or every other shot
        // is refused as NEED_BOLT (half the rated fire rate).
        int ammoCount = gun.getCurrentAmmoCount(gunStack);
        boolean chambered = gun.hasBulletInBarrel(gunStack);
        if (ammoCount > 0 && !chambered) {
            gun.setBulletInBarrel(gunStack, true);
        } else if (ammoCount <= 0 && chambered) {
            gun.setBulletInBarrel(gunStack, false);
        }
        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
        if (operator != null && operator.getDataHolder() != null) {
            if (operator.getDataHolder().isBolting) {
                com.taczammobox.util.AutoFireServer.DIAG.log("client-bolt", "cleared isBolting");
            }
            operator.getDataHolder().isBolting = false;
        }
    }
}
