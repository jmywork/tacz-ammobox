package com.taczammobox.network;

import com.taczammobox.config.FeatureFlags;
import com.taczammobox.config.WasteSettings;
import com.taczammobox.util.AutoFireTier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-authoritative copy of settings.json, features.json and autofire.json.
 *
 * <p>Sent on login and after /taczammobox reload so a client never runs with a different
 * feature set or magazine tuning than the server.</p>
 */
public record SettingsPacket(boolean waste, int packerTicks, int loaderTicks,
                             boolean autoFire, boolean capacityOverride, boolean rpmOverride,
                             boolean belt, boolean easterEggTab, boolean createCompat, boolean safeMode,
                             int[] capacities, int[] rpms) {

    private static final int TIERS = 3;

    public static void encode(SettingsPacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.waste);
        buffer.writeVarInt(message.packerTicks);
        buffer.writeVarInt(message.loaderTicks);
        buffer.writeBoolean(message.autoFire);
        buffer.writeBoolean(message.capacityOverride);
        buffer.writeBoolean(message.rpmOverride);
        buffer.writeBoolean(message.belt);
        buffer.writeBoolean(message.easterEggTab);
        buffer.writeBoolean(message.createCompat);
        buffer.writeBoolean(message.safeMode);
        for (int i = 0; i < TIERS; i++) {
            buffer.writeVarInt(message.capacities[i]);
            buffer.writeVarInt(message.rpms[i]);
        }
    }

    public static SettingsPacket decode(FriendlyByteBuf buffer) {
        boolean waste = buffer.readBoolean();
        int packer = buffer.readVarInt();
        int loader = buffer.readVarInt();
        boolean autoFire = buffer.readBoolean();
        boolean capacityOverride = buffer.readBoolean();
        boolean rpmOverride = buffer.readBoolean();
        boolean belt = buffer.readBoolean();
        boolean easterEggTab = buffer.readBoolean();
        boolean createCompat = buffer.readBoolean();
        boolean safeMode = buffer.readBoolean();
        int[] capacities = new int[TIERS];
        int[] rpms = new int[TIERS];
        for (int i = 0; i < TIERS; i++) {
            capacities[i] = buffer.readVarInt();
            rpms[i] = buffer.readVarInt();
        }
        return new SettingsPacket(waste, packer, loader, autoFire, capacityOverride, rpmOverride,
                belt, easterEggTab, createCompat, safeMode, capacities, rpms);
    }

    public static void handle(SettingsPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            WasteSettings.applyFromServer(message.waste, message.packerTicks, message.loaderTicks);
            FeatureFlags.applyFromServer(message.autoFire, message.capacityOverride, message.rpmOverride,
                    message.belt, message.easterEggTab, message.createCompat, message.safeMode);
            AutoFireTier.applyFromServer(message.capacities, message.rpms);
        });
        context.setPacketHandled(true);
    }
}
