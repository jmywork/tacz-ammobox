package com.taczammobox.util;

import com.mojang.authlib.GameProfile;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.compat.CuriosCompat;
import com.taczammobox.config.FeatureFlags;
import com.taczammobox.entity.SatelliteGunEntity;
import com.taczammobox.item.SatelliteRackItem;
import com.taczammobox.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Keeps the halo of satellite guns in sync with the rack the player wears and
 * turns the player's own shots into a volley from every satellite.
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID)
public final class SatelliteRackManager {
    /** Degrees away from "straight behind", alternating left/right. */
    private static final double[] OFFSETS = {14.0D, -14.0D, 29.0D, -29.0D, 44.0D, -44.0D, 59.0D, -59.0D, 74.0D, -74.0D};
    private static final double RADIUS = 2.4D;
    private static final double HEIGHT = 1.15D;
    private static final double AIM_DISTANCE = 16.0D;
    private static final GameProfile PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("taczammobox:satellite".getBytes(StandardCharsets.UTF_8)), "[Satellite]");
    private static boolean warned;

    private SatelliteRackManager() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        Player player = event.player;
        ItemStack rack = findRack(player);
        if (rack.isEmpty() || !FeatureFlags.easterEggTab()) {
            discard(player);
            return;
        }
        List<ItemStack> guns = SatelliteRackItem.loadedGuns(rack);
        List<SatelliteGunEntity> existing = satellites(player);
        if (guns.isEmpty()) {
            existing.forEach(SatelliteGunEntity::discard);
            return;
        }

        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(AIM_DISTANCE)).add(0.0D, -0.8D, 0.0D);
        for (int i = 0; i < guns.size(); i++) {
            SatelliteGunEntity entity;
            if (i < existing.size()) {
                entity = existing.get(i);
            } else {
                entity = new SatelliteGunEntity(ModEntities.SATELLITE_GUN.get(), player.level());
                entity.setOwner(player.getUUID());
                entity.setPos(player.getX(), player.getY() + HEIGHT, player.getZ());
                player.level().addFreshEntity(entity);
            }
            double angle = Math.toRadians(player.getYRot() + 180.0D + OFFSETS[i % OFFSETS.length]);
            double x = player.getX() - Math.sin(angle) * RADIUS;
            double z = player.getZ() + Math.cos(angle) * RADIUS;
            double y = player.getY() + HEIGHT + (i % 2 == 0 ? 0.18D : -0.12D);
            entity.setPos(x, y, z);

            double dx = target.x - x;
            double dy = target.y - y;
            double dz = target.z - z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
            entity.setYRot(yaw);
            entity.setXRot(Mth.clamp(pitch, -60.0F, 60.0F));
            entity.setRoll((i % 2 == 0 ? 9.0F : -9.0F));
            entity.setGun(guns.get(i));
            entity.setSlotIndex(i);
        }
        for (int i = guns.size(); i < existing.size(); i++) {
            existing.get(i).discard();
        }
    }

    @SubscribeEvent
    public static void onGunShoot(GunShootEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) {
            return;
        }
        if (!(event.getShooter() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        List<SatelliteGunEntity> satellites = satellites(player);
        if (satellites.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        for (SatelliteGunEntity satellite : satellites) {
            if (now < satellite.getNextFire()) {
                continue;
            }
            long interval = fire(satellite);
            if (interval > 0L) {
                satellite.setNextFire(now + interval);
            }
        }
    }

    /** @return the cooldown in ticks, or 0 when the shot did not happen. */
    private static long fire(SatelliteGunEntity satellite) {
        if (!(satellite.level() instanceof ServerLevel level)) {
            return 0L;
        }
        ItemStack gunStack = satellite.getGun().copy();
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return 0L;
        }
        // Easter egg: no ammo bookkeeping, every shot starts with a full chamber.
        gun.setCurrentAmmoCount(gunStack, 999);
        gun.setBulletInBarrel(gunStack, true);

        FakePlayer shooter = FakePlayerFactory.get(level, PROFILE);
        shooter.moveTo(satellite.getX(), satellite.getY(), satellite.getZ(), satellite.getYRot(), satellite.getXRot());
        shooter.setYHeadRot(satellite.getYRot());
        shooter.setItemInHand(InteractionHand.MAIN_HAND, gunStack);
        try {
            IGunOperator.fromLivingEntity(shooter).shoot(satellite::getXRot, satellite::getYRot);
        } catch (Throwable throwable) {
            if (!warned) {
                warned = true;
                throwable.printStackTrace();
            }
            return 0L;
        } finally {
            shooter.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        return TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack))
                .map(index -> index.getGunData().getShootInterval(shooter, gun.getFireMode(gunStack), gunStack))
                .orElse(200L);
    }

    private static List<SatelliteGunEntity> satellites(Player player) {
        List<SatelliteGunEntity> list = new ArrayList<>(player.level().getEntitiesOfClass(
                SatelliteGunEntity.class,
                player.getBoundingBox().inflate(10.0D),
                entity -> player.getUUID().equals(entity.getOwner())));
        list.sort(Comparator.comparingInt(SatelliteGunEntity::getSlotIndex));
        return list;
    }

    private static void discard(Player player) {
        for (SatelliteGunEntity entity : satellites(player)) {
            entity.discard();
        }
    }

    private static ItemStack findRack(Player player) {
        if (player.getMainHandItem().getItem() instanceof SatelliteRackItem) {
            return player.getMainHandItem();
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof SatelliteRackItem) {
                return stack;
            }
        }
        if (ModList.get().isLoaded("curios")) {
            return CuriosCompat.findCurio(player, stack -> stack.getItem() instanceof SatelliteRackItem);
        }
        return ItemStack.EMPTY;
    }
}
