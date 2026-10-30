package com.taczammobox.network;

import com.taczammobox.client.ClientConfigCheck;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ConfigSyncPacket(List<String> types) {

    public static void encode(ConfigSyncPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.types.size());
        for (String type : message.types) {
            buffer.writeUtf(type, 32767);
        }
    }

    public static ConfigSyncPacket decode(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), 4096);
        List<String> types = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            types.add(buffer.readUtf(32767));
        }
        return new ConfigSyncPacket(types);
    }

    public static void handle(ConfigSyncPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientConfigCheck.handle(message)));
        context.setPacketHandled(true);
    }
}
