package com.taczammobox.network;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.compat.ConfigCheck;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ConfigReplyPacket(List<String> types) {

    public static void encode(ConfigReplyPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.types.size());
        for (String type : message.types) {
            buffer.writeUtf(type, 32767);
        }
    }

    public static ConfigReplyPacket decode(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), 4096);
        List<String> types = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            types.add(buffer.readUtf(32767));
        }
        return new ConfigReplyPacket(types);
    }

    public static void handle(ConfigReplyPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            List<String> server = ConfigCheck.snapshot();
            List<String> missing = ConfigCheck.missing(server, message.types);
            List<String> extra = ConfigCheck.extra(server, message.types);
            if (missing.isEmpty() && extra.isEmpty()) {
                return;
            }
            TaczAmmoBoxMod.LOGGER.warn(
                    "Disconnecting {}: ammo config mismatch (client missing {}, client extra {}) {} {}",
                    player.getGameProfile().getName(), missing.size(), extra.size(), missing, extra);
            player.connection.disconnect(Component.translatable(
                    "message.taczammobox.config_kick", missing.size(), extra.size()));
        });
        context.setPacketHandled(true);
    }
}
