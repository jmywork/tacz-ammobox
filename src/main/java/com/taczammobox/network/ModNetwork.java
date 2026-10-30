package com.taczammobox.network;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.compat.ConfigCheck;
import com.taczammobox.config.WasteSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;

public final class ModNetwork {
    private static final String VERSION = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TaczAmmoBoxMod.MOD_ID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(FillPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FillPacket::encode)
                .decoder(FillPacket::decode)
                .consumerMainThread((message, contextSupplier) -> {
                    var context = contextSupplier.get();
                    message.handle(context.getSender());
                    context.setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(SettingsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SettingsPacket::encode)
                .decoder(SettingsPacket::decode)
                .consumerMainThread(SettingsPacket::handle)
                .add();

        CHANNEL.messageBuilder(ConfigReplyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigReplyPacket::encode)
                .decoder(ConfigReplyPacket::decode)
                .consumerMainThread(ConfigReplyPacket::handle)
                .add();

        CHANNEL.messageBuilder(ConfigSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfigSyncPacket::encode)
                .decoder(ConfigSyncPacket::decode)
                .consumerMainThread(ConfigSyncPacket::handle)
                .add();
    }

    public static void sendFill() {
        sendFill(-1, -1);
    }

    public static void sendFill(int slot, int containerId) {
        CHANNEL.sendToServer(new FillPacket(slot, containerId));
    }

    public static void sendSettings(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SettingsPacket(WasteSettings.waste(), WasteSettings.packerTicks(), WasteSettings.loaderTicks()));
    }

    public static void sendConfig(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ConfigSyncPacket(ConfigCheck.snapshot()));
    }

    public static void sendConfigReply(List<String> types) {
        CHANNEL.sendToServer(new ConfigReplyPacket(types));
    }
}
