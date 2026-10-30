package com.taczammobox.network;

import com.tacz.guns.api.item.IAmmo;
import com.taczammobox.item.PackagedAmmo;
import com.taczammobox.util.AmmoBoxHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code slot < 0} means "use the main hand"; otherwise the client is asking to
 * fill from the hovered slot of its currently open container.
 */
public record FillPacket(int slot, int containerId) {
    private static final int COOLDOWN_TICKS = 2;
    private static final Map<UUID, Long> LAST_FILL = new HashMap<>();

    public static void encode(FillPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.slot + 1);
        buffer.writeVarInt(message.containerId + 1);
    }

    public static FillPacket decode(FriendlyByteBuf buffer) {
        return new FillPacket(buffer.readVarInt() - 1, buffer.readVarInt() - 1);
    }

    public void handle(ServerPlayer player) {
        if (player == null) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST_FILL.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) {
            return;
        }
        LAST_FILL.put(player.getUUID(), now);

        if (slot < 0) {
            handleHeld(player);
        } else {
            handleSlot(player);
        }
    }

    private void handleHeld(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof PackagedAmmo pkg) {
            AmmoBoxHelper.fillAmmoBox(player.level(), player, stack, pkg.getType(), pkg.boxesPerItem());
        } else if (stack.getItem() instanceof IAmmo) {
            AmmoBoxHelper.fillFromLooseAmmo(player.level(), player, stack);
        }
    }

    private void handleSlot(ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.containerId != containerId || slot >= menu.slots.size()) {
            return;
        }
        Slot sourceSlot = menu.getSlot(slot);
        if (sourceSlot instanceof ResultSlot) {
            return;
        }
        ItemStack stack = sourceSlot.getItem();
        if (stack.getItem() instanceof PackagedAmmo pkg) {
            AmmoBoxHelper.fillAmmoBox(player.level(), player, stack, pkg.getType(), pkg.boxesPerItem());
        } else if (stack.getItem() instanceof IAmmo) {
            AmmoBoxHelper.fillFromLooseAmmo(player.level(), player, stack);
        } else {
            return;
        }
        sourceSlot.setChanged();
        menu.broadcastChanges();
    }

    public static void clear(UUID playerId) {
        LAST_FILL.remove(playerId);
    }
}
