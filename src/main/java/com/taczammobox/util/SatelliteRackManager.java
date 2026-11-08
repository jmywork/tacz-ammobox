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
import net.minecraft.core.particles.ParticleTypes;
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
    /** Halo geometry in degrees; the gap between ARC_END and ARC_START+360 is the bottom. */
    private static final double ARC_START = -55.0D;
    private static final double ARC_END = 235.0D;
    private static final double RING_RADIUS = 1.25D;
    private static final double BACK_OFFSET = 1.35D;
    private static final double CENTER_HEIGHT = 1.45D;
    /** How far the top of the ring leans forward over the player. */
    private static final double RING_TILT = 30.0D;
    /** Resting inward cant, so the halo looks like a guard rather than a fence. */
    private static final float IDLE_CANT_YAW = 12.0F;
    private static final float IDLE_CANT_PITCH = 9.0F;
    /** How long the satellites keep converging on the last hit point. */
    private static final long AIM_HOLD_TICKS = 12L;
    /** After the player fires, satellites keep engaging on their own for this long. */
    private static final long COMBAT_WINDOW_TICKS = 30L;
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

        // Upright halo: a vertical ring hanging behind the back, its plane facing
        // the way the player looks. The bottom of the ring is left empty so it
        // never sits directly behind the legs.
        float yawRad = (float) Math.toRadians(player.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        double centerX = player.getX() - forwardX * BACK_OFFSET;
        double centerY = player.getY() + CENTER_HEIGHT;
        double centerZ = player.getZ() - forwardZ * BACK_OFFSET;

        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(AIM_DISTANCE)).add(0.0D, -0.6D, 0.0D);
        int count = guns.size();
        for (int i = 0; i < count; i++) {
            SatelliteGunEntity entity;
            if (i < existing.size()) {
                entity = existing.get(i);
            } else {
                entity = new SatelliteGunEntity(ModEntities.SATELLITE_GUN.get(), player.level());
                entity.setOwner(player.getUUID());
                entity.setPos(player.getX(), player.getY() + CENTER_HEIGHT, player.getZ());
                player.level().addFreshEntity(entity);
            }

            double thetaDeg = count == 1 ? 90.0D : ARC_START + (ARC_END - ARC_START) * i / (count - 1.0D);
            double theta = Math.toRadians(thetaDeg);
            double across = Math.cos(theta) * RING_RADIUS;
            double rise = Math.sin(theta) * RING_RADIUS;
            // Lean the whole ring forward at the top so it cups the player
            // instead of hanging behind like a flat picture frame.
            double tilt = Math.toRadians(RING_TILT);
            double forwardLean = rise * Math.sin(tilt);
            double x = centerX + rightX * across + forwardX * forwardLean;
            double y = centerY + rise * Math.cos(tilt);
            double z = centerZ + rightZ * across + forwardZ * forwardLean;
            entity.setPos(x, y, z);

            long now = player.level().getGameTime();
            Vec3 aim = entity.getAimTarget();
            if (aim != null && now <= entity.getAimUntil()) {
                pointAt(entity, aim);
            } else {
                // Resting pose: follow the look direction, but cant each gun
                // toward the player's centre line.
                float cantYaw = across > 0.01D ? -IDLE_CANT_YAW : across < -0.01D ? IDLE_CANT_YAW : 0.0F;
                float cantPitch = rise > 0.01D ? -IDLE_CANT_PITCH : rise < -0.01D ? IDLE_CANT_PITCH : 0.0F;
                entity.setYRot(player.getYRot() + cantYaw);
                entity.setXRot(Mth.clamp(player.getXRot() + cantPitch, -80.0F, 80.0F));
            }
            entity.setRoll(0.0F);
            entity.setGun(guns.get(i));
            entity.setSlotIndex(i);

            // Each satellite keeps its own cadence inside the combat window.
            if (now < entity.getCombatUntil() && now >= entity.getNextFire()) {
                long interval = fire(entity);
                if (interval > 0L) {
                    entity.setNextFire(now + interval);
                    entity.setFlash(2);
                    muzzleFx(entity);
                }
            }
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
        // Everything the player is aiming at becomes the convergence point.
        Vec3 aim = player.pick(AIM_DISTANCE * 4.0D, 1.0F, false).getLocation();
        for (SatelliteGunEntity satellite : satellites) {
            satellite.setAimTarget(aim, now + AIM_HOLD_TICKS);
            satellite.setCombatUntil(now + COMBAT_WINDOW_TICKS);
            pointAt(satellite, aim);
        }
    }

    private static void muzzleFx(SatelliteGunEntity satellite) {
        if (!(satellite.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 look = satellite.getViewVector(1.0F);
        double mx = satellite.getX() + look.x * 0.45D;
        double my = satellite.getY() + look.y * 0.45D;
        double mz = satellite.getZ() + look.z * 0.45D;
        level.sendParticles(ParticleTypes.SMOKE, mx, my, mz, 2, 0.03D, 0.03D, 0.03D, 0.01D);
        level.sendParticles(ParticleTypes.FLAME, mx, my, mz, 1, 0.02D, 0.02D, 0.02D, 0.0D);
    }

    private static void pointAt(SatelliteGunEntity satellite, Vec3 target) {
        double dx = target.x - satellite.getX();
        double dy = target.y - satellite.getY();
        double dz = target.z - satellite.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        satellite.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        satellite.setXRot(Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, horizontal)), -80.0F, 80.0F));
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
            IGunOperator operator = IGunOperator.fromLivingEntity(shooter);
            // Aiming state tightens TACZ's inaccuracy modifiers a little; there is
            // no public API for a truly zero-spread shot, so this is as close as
            // we get without touching their internals.
            operator.aim(true);
            operator.shoot(satellite::getXRot, satellite::getYRot);
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
