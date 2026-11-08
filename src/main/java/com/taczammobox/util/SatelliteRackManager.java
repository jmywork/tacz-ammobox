package com.taczammobox.util;

import com.mojang.authlib.GameProfile;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.resource.pojo.data.gun.GunHeatData;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import org.joml.Vector2d;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.compat.CuriosCompat;
import com.taczammobox.config.FeatureFlags;
import com.taczammobox.entity.SatelliteGunEntity;
import com.taczammobox.item.SatelliteRackItem;
import com.taczammobox.registry.ModEntities;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    /** Enemies inside this radius get engaged automatically. */
    private static final double AUTO_RANGE = 24.0D;
    /** A marked target stays in focus out to this distance. */
    private static final double FOCUS_RANGE = 40.0D;
    /** The halo's own search cadence; scanning every tick is wasteful. */
    private static final long SCAN_INTERVAL = 4L;
    /** Hard floor so no gun can turn the halo into a bullet fountain (energy guns report 0 ms). */
    private static final long MIN_INTERVAL_TICKS = 2L;
    /** Live bullets per satellite before it stops adding more. */
    private static final int BULLET_BUDGET = 8;
    private static final GameProfile PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("taczammobox:satellite".getBytes(StandardCharsets.UTF_8)), "[Satellite]");
    /** Stand-in for "this happened long before the fake player's timeline". */
    private static final long LONG_AGO = -60_000L;
    /**
     * TACZ 1.2.0 gates every shot behind its own feed-type check
     * (FeedType.INVENTORY needs matching ammo in the shooter's inventory), which a
     * fake player cannot satisfy; the sentry arm breaks on 1.2.0 for the same
     * reason. So the halo builds its bullets itself and never asks TACZ to shoot.
     */
    private static final boolean USE_NATIVE_SHOOT = false;
    /**
     * Safety valve only: ten guns can never legitimately need more than a few
     * bullets in one tick, and this number is high enough not to touch real
     * cadences. TPS is protected by MAX_LIVE_BULLETS instead.
     */
    private static final int MAX_BULLETS_PER_TICK = 32;
    /** Live satellite bullets around one gun before it waits for them to land. */
    private static final int MAX_LIVE_BULLETS = 10;
    private static final Map<UUID, LivingEntity> TARGET_CACHE = new HashMap<>();
    private static final Map<UUID, Long> TARGET_TICK = new HashMap<>();
    private static final Map<UUID, int[]> SPAWN_BUDGET = new HashMap<>();
    private static boolean warned;
    private static boolean resultWarned;

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
        long tick = player.level().getGameTime();
        Long cachedAt = TARGET_TICK.get(player.getUUID());
        LivingEntity target;
        if (cachedAt == null || tick - cachedAt >= 4L) {
            target = findTarget(player);
            TARGET_CACHE.put(player.getUUID(), target);
            TARGET_TICK.put(player.getUUID(), tick);
        } else {
            target = TARGET_CACHE.get(player.getUUID());
            if (target != null && !target.isAlive()) {
                target = null;
            }
        }
        SPAWN_BUDGET.put(player.getUUID(), new int[] {MAX_BULLETS_PER_TICK});
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
            if (target != null) {
                pointAt(entity, target.getEyePosition());
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

            boolean shouldFire = target != null && now >= entity.getNextFire();
            if (player.level() instanceof ServerLevel serverLevel) {
                bindShooter(entity, serverLevel, guns.get(i), shouldFire);
            }

            coolHeat(entity);

            // Each satellite fires on its own gun's cadence.
            if (shouldFire) {
                long interval = fire(entity, target);
                if (interval > 0L) {
                    entity.setNextFire(now + interval);
                    entity.setFlash(2);
                    muzzleFx(entity);
                } else {
                    // Locked (heat, no target line, empty cache): back off a little
                    // instead of hammering the gun every tick.
                    entity.setNextFire(now + 2L);
                }
            }
        }
        for (int i = guns.size(); i < existing.size(); i++) {
            refundMagazine(existing.get(i));
            existing.get(i).discard();
        }
    }

    /** A marked target wins; otherwise the closest hostile with line of sight. */
    @Nullable
    private static LivingEntity findTarget(Player player) {
        ItemStack rack = findRack(player);
        UUID focus = SatelliteRackItem.focus(rack);
        if (focus != null) {
            var entity = player.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(focus) : null;
            if (entity instanceof LivingEntity living && isFocusValid(player, living, FOCUS_RANGE)) {
                return living;
            }
            if (!rack.isEmpty()) {
                // The mark died or walked away; drop it so normal search resumes.
                SatelliteRackItem.setFocus(rack, null);
            }
        }
        AABB box = player.getBoundingBox().inflate(AUTO_RANGE);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> isTargetCandidate(player, entity, AUTO_RANGE));
        candidates.sort(Comparator.comparingDouble(player::distanceToSqr));
        // Ray casts are the expensive part of the search, so only test the nearest
        // few candidates instead of every mob inside the box.
        int checked = 0;
        for (LivingEntity candidate : candidates) {
            if (player.hasLineOfSight(candidate)) {
                return candidate;
            }
            if (++checked >= 4) {
                break;
            }
        }
        return null;
    }

    /** A marked target can be anything living, hostiles or not. */
    private static boolean isFocusValid(Player player, LivingEntity entity, double range) {
        if (entity == player || !entity.isAlive() || entity.isSpectator() || entity instanceof Player) {
            return false;
        }
        return player.distanceToSqr(entity) <= range * range && player.hasLineOfSight(entity);
    }

    private static boolean isTargetCandidate(Player player, LivingEntity entity, double range) {
        if (entity == player || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (entity instanceof Player) {
            return false;
        }
        if (!(entity instanceof Enemy)) {
            return false;
        }
        if (entity instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }
        return player.distanceToSqr(entity) <= range * range;
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

    /**
     * TACZ's own shoot path ends in a script that bails out silently for entities
     * that never "drew" the gun, so build the bullet that script would have built:
     * the same EntityKineticBullet, the same gun data, our own direction.
     */
    private static final Map<String, Long> AMMO_NOTICE = new HashMap<>();

    /**
     * Chat notice when an upkeep gun has nothing to eat. Chat is persistent enough
     * for a player to actually act on it, but ten guns would spam it, so each
     * caliber is announced at most once every ten seconds.
     */
    private static void noticeMissingAmmo(@Nullable Player player, ResourceLocation ammoId) {
        if (player == null) {
            return;
        }
        long now = player.level().getGameTime();
        String key = player.getUUID() + "/" + ammoId;
        Long last = AMMO_NOTICE.put(key, now);
        if (last != null && now - last < 200L) {
            return;
        }
        player.sendSystemMessage(Component.translatable("message.taczammobox.satellite.no_ammo",
                ammoName(ammoId)));
    }

    /**
     * The pack index carries the translation key of the caliber ("tacz.ammo.556x45.name"),
     * so the chat notice can use the same colored name the tooltip shows instead of a
     * raw id. Packs that forget the key fall back to the id path.
     */
    private static Component ammoName(ResourceLocation ammoId) {
        try {
            CommonAmmoIndex index = TimelessAPI.getCommonAmmoIndex(ammoId).orElse(null);
            if (index != null && index.getPojo() != null) {
                String key = index.getPojo().getName();
                if (key != null && !key.isBlank()) {
                    return Component.translatable(key);
                }
            }
        } catch (Throwable ignored) {
            // A broken third-party pack must not take the notice down with it.
        }
        return Component.literal(ammoId.getPath().toUpperCase(java.util.Locale.ROOT));
    }

    /** Return unfired rounds to the rack when a satellite goes away. */
    private static void refundMagazine(SatelliteGunEntity satellite) {
        if (!(satellite.level() instanceof ServerLevel level) || satellite.getMagazine() <= 0) {
            return;
        }
        Player player = level.getPlayerByUUID(satellite.getOwner());
        if (player == null) {
            return;
        }
        ItemStack rack = findRack(player);
        if (rack.isEmpty() || !SatelliteRackItem.consumesAmmo(rack)) {
            return;
        }
        IGun gun = IGun.getIGunOrNull(satellite.getGun());
        if (gun == null) {
            return;
        }
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(gun.getGunId(satellite.getGun())).orElse(null);
        if (index == null || index.getGunData().getAmmoId() == null) {
            return;
        }
        SatelliteAmmo.refund(rack, index.getGunData().getAmmoId(), satellite.getMagazine());
        satellite.setMagazine(0);
    }

    /** Bleed heat off after the pack's cooling delay; unsynced, server only. */
    private static void coolHeat(SatelliteGunEntity satellite) {
        if (!(satellite.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack stack = satellite.getGun();
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null || !gun.hasHeatData(stack)) {
            satellite.setHeat(0.0F);
            return;
        }
        GunHeatData heatData = TimelessAPI.getCommonGunIndex(gun.getGunId(stack))
                .map(index -> index.getGunData().getHeatData()).orElse(null);
        if (heatData == null || satellite.getHeat() <= 0.0F) {
            return;
        }
        long delay = Math.max(0L, heatData.getCoolingDelay() / 50L);
        if (level.getGameTime() - satellite.getLastShotTick() < delay) {
            return;
        }
        satellite.setHeat(Math.max(0.0F, satellite.getHeat() - Math.max(0.05F, heatData.getInventoryCoolingPerTick())));
    }

    private static boolean spawnBullet(ServerLevel level, FakePlayer shooter, LivingEntity owner,
                                       SatelliteGunEntity satellite, ItemStack gunStack, IGun gun, Vec3 dir,
                                       @Nullable LivingEntity target) {
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack)).orElse(null);
        if (index == null) {
            return false;
        }
        GunData gunData = index.getGunData();
        BulletData bulletData = index.getBulletData();
        if (gunData == null || bulletData == null || gunData.getAmmoId() == null) {
            return false;
        }
        // The constructor reads the shooter's attachment cache and throws when it is
        // null, so build it on the fake (which we keep warm); the player becomes the
        // owner right after, which fixes damage attribution and stops our own bullets
        // from colliding with the player standing in front of the ring.
        int[] budget = SPAWN_BUDGET.get(owner.getUUID());
        if (budget != null) {
            if (budget[0] <= 0) {
                return false;
            }
            budget[0]--;
        }
        long tickNow = level.getGameTime();
        long checkedAt = satellite.getBulletCheckTick();
        if (tickNow - checkedAt >= 10L) {
            int live = level.getEntitiesOfClass(EntityKineticBullet.class,
                    satellite.getBoundingBox().inflate(16.0D),
                    bullet -> bullet.getOwner() == owner).size();
            satellite.setLiveBullets(live);
            satellite.setBulletCheckTick(tickNow);
        }
        if (satellite.getLiveBullets() >= MAX_LIVE_BULLETS) {
            satellite.setLiveBullets(satellite.getLiveBullets() - 1);
            return false;
        }
        EntityKineticBullet bullet = new EntityKineticBullet(level, shooter, gunStack, gunData.getAmmoId(),
                gun.getGunId(gunStack), true, gunData, bulletData);
        if (owner != shooter) {
            bullet.setOwner(owner);
        }
        Vec3 muzzle = satellite.position().add(0.0D, 0.35D, 0.0D).add(dir.scale(0.6D));
        bullet.setPos(muzzle.x, muzzle.y, muzzle.z);
        float speed = Math.max(1.0F, bulletData.getSpeed());
        bullet.shootFromRotation(owner, satellite.getXRot(), satellite.getYRot(), 0.0F, speed,
                new Vector2d(0.0D, 0.0D));
        if (target != null) {
            // Aim from the actual muzzle at the torso and lead a walking mob: the
            // angle-only shot drifts behind a moving target because the ring sits
            // ~1.5 blocks behind the player. Overriding the velocity vector makes the
            // trajectory independent of any yaw/pitch convention mismatch.
            Vec3 aim = target.getBoundingBox().getCenter();
            double distance = muzzle.distanceTo(aim);
            double seconds = distance / speed;
            aim = aim.add(target.getDeltaMovement().scale(Math.min(6.0D, seconds * 20.0D)));
            Vec3 delta = aim.subtract(muzzle);
            if (delta.lengthSqr() > 1.0E-6D) {
                Vec3 velocity = delta.normalize().scale(speed);
                bullet.setDeltaMovement(velocity);
                bullet.setYRot((float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)));
                bullet.setXRot((float) -Math.toDegrees(Math.atan2(velocity.y,
                        Math.hypot(velocity.x, velocity.z))));
            }
        }
        return level.addFreshEntity(bullet);
    }


    /** Keeps the fake player's TACZ state (aim, draw, heat) alive between shots. */
    private static void bindShooter(SatelliteGunEntity satellite, ServerLevel level, ItemStack gunStack,
                                    boolean firing) {
        FakePlayer shooter = satellite.shooter(level);
        shooter.moveTo(satellite.getX(), satellite.getY(), satellite.getZ(), satellite.getYRot(), satellite.getXRot());
        shooter.setYHeadRot(satellite.getYRot());
        shooter.setXRot(satellite.getXRot());
        // A full player tick ten times a game tick was the biggest TPS sink here:
        // the bullets are built by us, so the fake player only has to stay a valid
        // TACZ subject with a warm attachment cache.
        IGunOperator operator = IGunOperator.fromLivingEntity(shooter);
        if (operator.getCacheProperty() == null) {
            AttachmentPropertyManager.postChangeEvent(shooter, gunStack);
        }
        if (gunStack.getItem() instanceof AbstractGunItem gunItem) {
            ShooterDataHolder data = operator.getDataHolder();
            gunItem.tickHeat(data, gunStack, shooter);
        }
    }

    private static long fire(SatelliteGunEntity satellite, @Nullable LivingEntity target) {
        if (!(satellite.level() instanceof ServerLevel level)) {
            return 0L;
        }
        ItemStack gunStack = satellite.getGun().copy();
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return 0L;
        }
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack)).orElse(null);
        GunData gunData = index == null ? null : index.getGunData();
        Player player = level.getPlayerByUUID(satellite.getOwner());
        ItemStack rack = player == null ? ItemStack.EMPTY : findRack(player);
        boolean upkeep = !rack.isEmpty() && SatelliteRackItem.consumesAmmo(rack);
        int magazine = satellite.getMagazine();
        if (upkeep && gunData != null && gunData.getAmmoId() != null) {
            int magSize = Math.max(1, gunData.getAmmoAmount());
            if (magazine <= 0) {
                magazine = SatelliteAmmo.draw(player, rack, gunData.getAmmoId(), magSize);
                satellite.setMagazine(magazine);
            }
            if (magazine <= 0) {
                noticeMissingAmmo(player, gunData.getAmmoId());
                return 0L;
            }
            gun.setCurrentAmmoCount(gunStack, magazine);
            gun.setBulletInBarrel(gunStack, true);
        } else {
            // Easter egg: no ammo bookkeeping, every shot starts with a full chamber.
            gun.setCurrentAmmoCount(gunStack, 999);
            gun.setBulletInBarrel(gunStack, true);
        }

        long nowTick = level.getGameTime();
        if (gun.hasHeatData(gunStack)) {
            if (nowTick < satellite.getHeatLockUntil()) {
                return 0L;
            }
            GunHeatData heatData = TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack))
                    .map(gunIndex -> gunIndex.getGunData().getHeatData()).orElse(null);
            if (heatData != null) {
                float nextHeat = satellite.getHeat() + heatData.getHeatPerShot();
                if (nextHeat >= heatData.getHeatMax()) {
                    // Overheated: TACZ locks the real gun for a moment; the halo has to
                    // do the same or it keeps pouring bullets into the world.
                    satellite.setHeat(0.0F);
                    satellite.setHeatLockUntil(nowTick + Math.max(10L, heatData.getOverHeatTime() / 50L));
                    return 0L;
                }
                satellite.setHeat(nextHeat);
                satellite.setLastShotTick(nowTick);
            }
        }

        FakePlayer shooter = satellite.shooter(level);
        shooter.moveTo(satellite.getX(), satellite.getY(), satellite.getZ(), satellite.getYRot(), satellite.getXRot());
        shooter.setYHeadRot(satellite.getYRot());
        shooter.setItemInHand(InteractionHand.MAIN_HAND, gunStack);
        try {
            IGunOperator operator = IGunOperator.fromLivingEntity(shooter);
            // A fake player never receives the timestamp sync a real client sends,
            // so TACZ's anti-cheat rejects every shot as NETWORK_FAIL unless we
            // seed the holder ourselves.
            ShooterDataHolder data = operator.getDataHolder();
            data.shootTimestamp = -1L;
            data.lastShootTimestamp = -1L;
            data.drawTimestamp = -1L;
            data.reloadTimestamp = -1L;
            data.boltTimestamp = -1L;
            data.reloadStateType = ReloadState.StateType.NOT_RELOADING;
            data.sprintTimeS = 0.0F;
            data.isBolting = false;
            data.setCurrentGunItem(() -> gunStack);
            // Aiming state tightens TACZ's inaccuracy modifiers a little; there is
            // no public API for a truly zero-spread shot, so this is as close as
            // we get without touching their internals.
            operator.aim(true);
            // IGunOperator.shoot() runs the whole real-player gate first (cooldown,
            // base-timestamp network window, draw, bolt, ammo). A fake player has no
            // client to sync any of that, and seeding the holder did not satisfy the
            // gate, so call the spawner that gate eventually delegates to and skip
            // the gate entirely. Ammo is refilled above, so nothing is consumed.
            // shootOnce() bails out silently when the operator has no attachment
            // cache, and a fake player never receives the event that builds it, so
            // rebuild it here before asking the gun to fire.
            if (operator.getCacheProperty() == null) {
                AttachmentPropertyManager.postChangeEvent(shooter, gunStack);
            }
            // aim()/postChangeEvent() may replace tacz$data with a fresh holder,
            // so re-read it and stamp the *current* instance as late as possible.
            data = operator.getDataHolder();
            // Never touch baseTimestamp: TACZ computes the shoot timestamp as
            // GunTime.nowMillis() - baseTimestamp, and baseTimestamp lives in that
            // clock's domain. Overwriting it with System.currentTimeMillis() made the
            // timestamp hugely negative, so the cooldown looked like a million ms.
            // Negative sentinels mean "never fired", which is exactly our state.
            data.shootTimestamp = -1L;
            data.lastShootTimestamp = -1L;
            data.drawTimestamp = -1L;
            data.reloadTimestamp = -1L;
            data.boltTimestamp = -1L;
            data.reloadStateType = ReloadState.StateType.NOT_RELOADING;
            data.isBolting = false;
            data.setCurrentGunItem(() -> gunStack);
            Vec3 dir = Vec3.directionFromRotation(satellite.getXRot(), satellite.getYRot());
            ServerPlayer owner = level.getPlayerByUUID(satellite.getOwner()) instanceof ServerPlayer serverPlayer
                    ? serverPlayer : null;
            boolean spawned = spawnBullet(level, shooter, owner == null ? shooter : owner, satellite, gunStack,
                    gun, dir, target);
            if (upkeep && spawned) {
                magazine = Math.max(0, magazine - 1);
                satellite.setMagazine(magazine);
                gun.setCurrentAmmoCount(gunStack, magazine);
            }
        } catch (Throwable throwable) {
            if (!warned) {
                warned = true;
                throwable.printStackTrace();
            }
            return 0L;
        } finally {
            shooter.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        // GunData.getShootInterval() is in milliseconds, not ticks.
        long intervalMs = gunData != null
                ? gunData.getShootInterval(shooter, gun.getFireMode(gunStack), gunStack)
                : 200L;
        // A zero interval means the pack drives the gun through a Lua script (most
        // energy weapons); we cannot run that script, so give them a slow cadence.
        long interval = intervalMs <= 0L ? 4L : Math.max(MIN_INTERVAL_TICKS, intervalMs / 50L);
        return interval;
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

    public static ItemStack findRack(Player player) {
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
