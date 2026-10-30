package com.taczammobox.network;

import com.taczammobox.config.WasteSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-authoritative copy of settings.json, sent on login and after reload so
 * the client's GUI progress bar and waste display match the server.
 */
public record SettingsPacket(boolean waste, int packerTicks, int loaderTicks) {

    public static void encode(SettingsPacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.waste);
        buffer.writeVarInt(message.packerTicks);
        buffer.writeVarInt(message.loaderTicks);
    }

    public static SettingsPacket decode(FriendlyByteBuf buffer) {
        return new SettingsPacket(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SettingsPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> WasteSettings.applyFromServer(
                message.waste, message.packerTicks, message.loaderTicks));
        context.setPacketHandled(true);
    }
}
