package com.taczammobox.client;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.compat.ConfigCheck;
import com.taczammobox.network.ConfigSyncPacket;
import com.taczammobox.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Client side of the login config check. */
public final class ClientConfigCheck {
    private ClientConfigCheck() {
    }

    public static void handle(ConfigSyncPacket packet) {
        List<String> local = ConfigCheck.snapshot();
        List<String> missing = ConfigCheck.missing(packet.types(), local);
        List<String> extra = ConfigCheck.extra(packet.types(), local);
        if (!missing.isEmpty() || !extra.isEmpty()) {
            TaczAmmoBoxMod.LOGGER.warn("Ammo config mismatch: client missing {} {}, client extra {} {}",
                    missing.size(), missing, extra.size(), extra);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable(
                        "message.taczammobox.config_mismatch", missing.size(), extra.size()), false);
            }
        }
        ModNetwork.sendConfigReply(local);
    }
}
