package com.taczammobox.network;

import com.taczammobox.util.AmmoBoxHelper;
import com.taczammobox.TaczAmmoBoxMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Client asks the server to move loose ammo from its inventory/Curios/open container into boxes. */
public record SortPacket() {
    private static final int COOLDOWN_TICKS = 5;
    private static final Map<UUID, Long> LAST_SORT = new HashMap<>();

    public static void encode(SortPacket message, FriendlyByteBuf buffer) {
    }

    public static SortPacket decode(FriendlyByteBuf buffer) {
        return new SortPacket();
    }

    public void handle(ServerPlayer player) {
        if (player == null) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST_SORT.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) {
            return;
        }
        LAST_SORT.put(player.getUUID(), now);
        int packed = AmmoBoxHelper.packLooseAmmo(player.level(), player);
        TaczAmmoBoxMod.LOGGER.info("Pack key by {}: {} boxes", player.getGameProfile().getName(), packed);
        if (player.containerMenu != null) {
            player.containerMenu.broadcastChanges();
        }
    }

    public static void clear(UUID playerId) {
        LAST_SORT.remove(playerId);
    }
}
