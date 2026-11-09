package com.taczammobox.network;

import com.taczammobox.item.SatelliteRackItem;
import com.taczammobox.util.SatelliteRackManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Carries the "focus this target" keybind. Id 0 clears the mark, which is also
 * what happens when the player presses the key with nothing under the crosshair.
 */
public record FocusTargetPacket(int entityId) {
    public static void encode(FocusTargetPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    public static FocusTargetPacket decode(FriendlyByteBuf buffer) {
        return new FocusTargetPacket(buffer.readVarInt());
    }

    public static void handle(FocusTargetPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            ItemStack rack = SatelliteRackManager.findRack(player);
            if (!rack.isEmpty()) {
                if (packet.entityId == 0) {
                    LivingEntity looked = pickByRay(player);
                    if (looked != null) {
                        SatelliteRackItem.setFocus(rack, looked.getUUID());
                        player.displayClientMessage(Component.translatable("message.taczammobox.focus.set",
                                looked.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
                    } else {
                        SatelliteRackItem.setFocus(rack, null);
                        player.displayClientMessage(Component.translatable("message.taczammobox.focus.cleared")
                                .withStyle(ChatFormatting.GRAY), true);
                    }
                } else {
                    Entity entity = player.level().getEntity(packet.entityId);
                    if (entity instanceof LivingEntity living && living.isAlive()
                            && player.distanceToSqr(living) <= 64.0D * 64.0D) {
                        SatelliteRackItem.setFocus(rack, living.getUUID());
                        player.displayClientMessage(Component.translatable("message.taczammobox.focus.set",
                                living.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
                    } else {
                        player.displayClientMessage(Component.translatable("message.taczammobox.focus.none")
                                .withStyle(ChatFormatting.GRAY), true);
                    }
                }
            }
        }
        context.setPacketHandled(true);
    }

    /**
     * Server-side fallback: the client's picker ignores entities that are not
     * "pickable" (some dummies and armour stands), so cast our own box ray.
     */
    private static LivingEntity pickByRay(ServerPlayer player) {
        var eye = player.getEyePosition();
        var look = player.getLookAngle();
        var end = eye.add(look.scale(64.0D));
        var box = player.getBoundingBox().expandTowards(look.scale(64.0D)).inflate(1.0D);
        LivingEntity best = null;
        double bestDistance = 64.0D * 64.0D;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator())) {
            var hit = candidate.getBoundingBox().inflate(0.3D).clip(eye, end);
            if (hit.isPresent()) {
                double distance = eye.distanceToSqr(hit.get());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        }
        return best;
    }
}
