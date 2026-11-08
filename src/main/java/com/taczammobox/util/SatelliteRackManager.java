package com.taczammobox.util;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.BurstData;
import com.tacz.guns.resource.pojo.data.gun.ChargeData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunHeatData;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.compat.CuriosCompat;
import com.taczammobox.config.FeatureFlags;
import com.taczammobox.entity.SatelliteGunEntity;
import com.taczammobox.item.SatelliteRackItem;
import com.taczammobox.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps the halo of satellite guns in sync with the rack the player wears and
 * turns the player's own shots into a volley from every satellite.
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID)
public final class SatelliteRackManager {
    /**
     * Idle pose: one fan of guns behind the back. Every gun hangs off a single
     * pivot at the small of the back and radiates up and outwards, the way the
     * petals of the Huawei mark open — a peacock tail rather than a pair of wings.
     * The fan lies in the player's back plane, so from behind every gun shows its
     * broadside.
     */
    private static final double FAN_BACK_OFFSET = 0.60D;
    private static final double FAN_PIVOT_HEIGHT = 1.25D;
    private static final double FAN_RADIUS = 1.10D;
    /** Total opening angle in degrees, symmetric around straight up. */
    private static final double FAN_ARC = 170.0D;
    /** Roll that lays each gun broadside to the viewer instead of edge-on. */
    private static final float FAN_ROLL = 90.0F;
    /** Spawn height for a fresh entity, before the first layout pass moves it. */
    private static final double CENTER_HEIGHT = 1.45D;
    /** Enemies inside this radius get engaged automatically. */
    private static final double AUTO_RANGE = 24.0D;
    /** A marked target stays in focus out to this distance. */
    private static final double FOCUS_RANGE = 40.0D;
    /** The halo's own search cadence; scanning every tick is wasteful. */
    private static final long SCAN_INTERVAL = 4L;
    /**
     * 1200 RPM is TACZ's own ceiling and works out to one round per tick, so the
     * halo never needs to wait longer than that between shots.
     */
    private static final long MIN_INTERVAL_TICKS = 1L;
    /**
     * Safety valve only: ten guns can never legitimately need more than a few
     * bullets in one tick, and this number is high enough not to touch real
     * cadences. TPS is protected by MAX_LIVE_BULLETS instead.
     */
    private static final int MAX_BULLETS_PER_TICK = 32;
    /** Live satellite bullets around one gun before it waits for them to land. */
    private static final int MAX_LIVE_BULLETS = 10;
    private static final Map<UUID, int[]> SPAWN_BUDGET = new HashMap<>();
    private static boolean warned;

    private SatelliteRackManager() {
    }

    /** Drop every per-player entry so a long running server does not accumulate them. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        SPAWN_BUDGET.remove(id);
        AMMO_NOTICE.keySet().removeIf(key -> key.startsWith(id + "/"));
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
        UUID playerId = player.getUUID();
        int[] budget = SPAWN_BUDGET.get(playerId);
        if (budget == null) {
            budget = new int[1];
            SPAWN_BUDGET.put(playerId, budget);
        }
        budget[0] = MAX_BULLETS_PER_TICK;
        // Guns that already picked a mob this tick: the next gun prefers a prey
        // nobody is shooting yet, so ten guns spread over a group instead of
        // stacking every barrel on the same one.
        Map<UUID, Integer> claims = new HashMap<>();
        List<ItemStack> guns = SatelliteRackItem.loadedGuns(rack);
        List<SatelliteGunEntity> existing = satellites(player);
        if (guns.isEmpty()) {
            existing.forEach(SatelliteGunEntity::discard);
            return;
        }

        // Peacock fan: the guns hang off one pivot at the small of the back and
        // spread across the arc, so the fan opens as a single shape. With ten guns
        // the inner pair stands almost upright and the outer pair lies out to the
        // sides; with one gun it sits dead centre.
        float yawRad = (float) Math.toRadians(player.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        double pivotX = player.getX() - forwardX * FAN_BACK_OFFSET;
        double pivotY = player.getY() + FAN_PIVOT_HEIGHT;
        double pivotZ = player.getZ() - forwardZ * FAN_BACK_OFFSET;

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

            double fraction = count <= 1 ? 0.5D : i / (double) (count - 1);
            double angle = Math.toRadians(-FAN_ARC / 2.0D + fraction * FAN_ARC);
            double across = Math.sin(angle) * FAN_RADIUS;
            double rise = Math.cos(angle) * FAN_RADIUS;
            double x = pivotX + rightX * across;
            double y = pivotY + rise;
            double z = pivotZ + rightZ * across;
            entity.setPos(x, y, z);

            long now = player.level().getGameTime();
            // Every gun chooses for itself; a marked target overrides all of them.
            LivingEntity prey = gunTarget(entity, player, rack, now, claims);
            if (prey != null) {
                claims.merge(prey.getUUID(), 1, Integer::sum);
                // Track the prey with the muzzle; the bullet is aimed by velocity,
                // so this is purely visual.
                pointAt(entity, prey.getEyePosition());
            } else {
                // Idle: the muzzle points along its own petal, so the fan reads as
                // one opening shape instead of a bundle of parallel guns.
                double dirX = rightX * Math.sin(angle);
                double dirY = Math.cos(angle);
                double dirZ = rightZ * Math.sin(angle);
                entity.setYRot((float) Math.toDegrees(Math.atan2(-dirX, dirZ)));
                entity.setXRot((float) -Math.toDegrees(Math.atan2(dirY, Math.hypot(dirX, dirZ))));
            }
            // In the FIXED frame a gun is already broadside when its muzzle points
            // sideways, so the petal guns must not be rolled at all; only the gun
            // aiming straight up sits edge-on and needs the quarter turn.
            entity.setRoll(Math.abs(angle) < 1.0E-4D ? FAN_ROLL : 0.0F);
            entity.setGunIfChanged(guns.get(i));
            entity.setSlotIndex(i);

            boolean shouldFire = prey != null && now >= entity.getNextFire();

            coolHeat(entity);

            // Each satellite fires on its own gun's cadence.
            if (shouldFire) {
                long interval = fire(entity, prey, player, rack);
                if (interval > 0L) {
                    entity.setNextFire(now + interval);
                    // The muzzle flash itself is TACZ's own, drawn by the renderer
                    // from this flag; no extra particle packets are sent.
                    entity.setFlash(2);
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

    /**
     * What this one gun should shoot at.
     *
     * <p>A marked target beats everything and pulls the whole fan onto it. Without
     * a mark every gun picks its own prey: the nearest hostile it can actually see,
     * preferring one the other guns have not claimed yet. Scans are staggered by
     * slot so ten guns never ray-cast on the same tick.</p>
     */
    @Nullable
    private static LivingEntity gunTarget(SatelliteGunEntity satellite, Player player, ItemStack rack,
                                          long tick, Map<UUID, Integer> claims) {
        UUID focusId = SatelliteRackItem.focus(rack);
        LivingEntity cached = satellite.getTarget();
        if (cached != null && (!cached.isAlive() || cached.level() != satellite.level())) {
            cached = null;
            satellite.setTarget(null);
        }
        if (focusId != null) {
            if (cached != null && focusId.equals(cached.getUUID())) {
                return cached;
            }
            var marked = player.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(focusId) : null;
            if (marked instanceof LivingEntity living && isFocusValid(player, living, FOCUS_RANGE)) {
                satellite.setTarget(living);
                satellite.setTargetCheckTick(tick + SCAN_INTERVAL);
                return living;
            }
            // The mark died or walked away; drop it so every gun searches again.
            SatelliteRackItem.setFocus(rack, null);
            satellite.setTarget(null);
            cached = null;
        }
        if (cached != null && tick < satellite.getTargetCheckTick()) {
            return cached;
        }
        LivingEntity found = searchTarget(satellite, player, claims);
        satellite.setTarget(found);
        satellite.setTargetCheckTick(tick + stagger(satellite.getSlotIndex()));
        return found;
    }

    /** Scans are offset per slot so the ray casts spread over the interval. */
    private static long stagger(int slot) {
        int step = (int) Math.max(1L, SCAN_INTERVAL);
        return SCAN_INTERVAL + Math.floorMod(slot, step);
    }

    /**
     * The nearest hostile around this gun that it can see, favouring prey no other
     * gun has claimed yet so a group of mobs gets shared out.
     */
    @Nullable
    private static LivingEntity searchTarget(SatelliteGunEntity satellite, Player player, Map<UUID, Integer> claims) {
        AABB box = satellite.getBoundingBox().inflate(AUTO_RANGE);
        List<LivingEntity> candidates = satellite.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> isTargetCandidate(satellite, player, entity));
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator.comparingDouble(satellite::distanceToSqr));
        LivingEntity best = null;
        int bestClaims = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        // Ray casts are the expensive part, so only test the nearest handful.
        int checked = 0;
        for (LivingEntity candidate : candidates) {
            if (checked++ >= 6) {
                break;
            }
            if (!canSee(satellite, candidate)) {
                continue;
            }
            int claimCount = claims.getOrDefault(candidate.getUUID(), 0);
            double distance = satellite.distanceToSqr(candidate);
            if (claimCount < bestClaims || (claimCount == bestClaims && distance < bestDistance)) {
                best = candidate;
                bestClaims = claimCount;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Block ray from the gun's own position, so a gun behind a wall stays quiet. */
    private static boolean canSee(SatelliteGunEntity satellite, LivingEntity target) {
        Vec3 from = satellite.position().add(0.0D, 0.2D, 0.0D);
        Vec3 to = target.getEyePosition();
        var hit = satellite.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, satellite));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** A marked target can be anything living, hostiles or not. */
    private static boolean isFocusValid(Player player, LivingEntity entity, double range) {
        if (entity == player || !entity.isAlive() || entity.isSpectator() || entity instanceof Player) {
            return false;
        }
        return player.distanceToSqr(entity) <= range * range;
    }

    private static boolean isTargetCandidate(SatelliteGunEntity satellite, Player player, LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (entity instanceof Player) {
            return false;
        }
        if (!(entity instanceof Enemy)) {
            return false;
        }
        UUID owner = satellite.getOwner();
        if (entity instanceof OwnableEntity ownable && owner != null && owner.equals(ownable.getOwnerUUID())) {
            return false;
        }
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }
        return satellite.distanceToSqr(entity) <= AUTO_RANGE * AUTO_RANGE;
    }

    /** Turns the gun's muzzle onto a point; the bullet is aimed separately. */
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
        // Most guns never build heat; skip the gun and pack lookups entirely then.
        if (satellite.getHeat() <= 0.0F) {
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
        if (heatData == null) {
            return;
        }
        long delay = Math.max(0L, heatData.getCoolingDelay() / 50L);
        if (level.getGameTime() - satellite.getLastShotTick() < delay) {
            return;
        }
        satellite.setHeat(Math.max(0.0F, satellite.getHeat() - Math.max(0.05F, heatData.getInventoryCoolingPerTick())));
    }

    private static boolean spawnBullet(ServerLevel level, FakePlayer shooter, LivingEntity owner,
                                       SatelliteGunEntity satellite, ItemStack gunStack, IGun gun,
                                       @Nullable CommonGunIndex index, Vec3 dir,
                                       @Nullable LivingEntity target) {
        GunData gunData = index == null ? null : index.getGunData();
        BulletData bulletData = index == null ? null : index.getBulletData();
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
        if (target == null) {
            bullet.shootFromRotation(owner, satellite.getXRot(), satellite.getYRot(), 0.0F, speed,
                    new Vector2d(0.0D, 0.0D));
        } else {
            // Aim from the actual muzzle at the torso and lead a walking mob: the
            // angle-only shot drifts behind a moving target because the fan sits
            // behind the player. Setting the velocity vector directly makes the
            // trajectory independent of any yaw/pitch convention mismatch, and
            // skips the angle shot that would only be overwritten.
            Vec3 aim = target.getBoundingBox().getCenter();
            double distance = muzzle.distanceTo(aim);
            double seconds = distance / speed;
            aim = aim.add(target.getDeltaMovement().scale(Math.min(6.0D, seconds * 20.0D)));
            Vec3 delta = aim.subtract(muzzle);
            Vec3 velocity = delta.lengthSqr() > 1.0E-6D ? delta.normalize().scale(speed) : dir.scale(speed);
            bullet.setDeltaMovement(velocity);
            bullet.setYRot((float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)));
            bullet.setXRot((float) -Math.toDegrees(Math.atan2(velocity.y,
                    Math.hypot(velocity.x, velocity.z))));
        }
        return level.addFreshEntity(bullet);
    }


    private static long fire(SatelliteGunEntity satellite, @Nullable LivingEntity target, Player player,
                             ItemStack rack) {
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
        boolean upkeep = player != null && !rack.isEmpty() && SatelliteRackItem.consumesAmmo(rack);
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
            GunHeatData heatData = gunData == null ? null : gunData.getHeatData();
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
                    gun, index, dir, target);
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
        // TACZ reports the trigger interval in milliseconds; shotIntervalTicks() also
        // folds in the bolt/charge action time and converts to ticks.
        long interval = shotIntervalTicks(gunData, gun, gunStack, shooter);
        if (interval != satellite.getLoggedInterval()) {
            satellite.setLoggedInterval(interval);
            // One line per gun, so a report can compare the halo's cadence with the
            // gun's own rate instead of guessing whether the TACZ interval was used.
            TaczAmmoBoxMod.LOGGER.info("[taczammobox] satellite {} cadence: {} tick(s) ~ {} RPM",
                    gun.getGunId(gunStack), interval, 1200L / Math.max(1L, interval));
        }
        return interval;
    }

    /**
     * Ticks this gun must wait between rounds when it fires the way TACZ fires it.
     *
     * <p>{@code GunData.getShootInterval()} only carries the trigger interval, so a
     * bolt gun would otherwise fire at its RPM while a player has to cycle the bolt,
     * and a charge gun would skip the charge completely. The action times are folded
     * in here. Our own tier magazines deliberately rewrite the interval and skip the
     * bolt, so their value is taken at face value.</p>
     */
    private static long shotIntervalTicks(@Nullable GunData gunData, IGun gun, ItemStack gunStack,
                                          FakePlayer shooter) {
        if (gunData == null) {
            return 4L;
        }
        long intervalMs;
        try {
            intervalMs = gunData.getShootInterval(shooter, gun.getFireMode(gunStack), gunStack);
        } catch (Throwable throwable) {
            // A broken attachment cache must not stop the halo from firing.
            intervalMs = 0L;
        }
        if (intervalMs <= 0L) {
            // The pack drives this gun through a Lua script (most energy weapons);
            // we cannot run that script, so give it a slow cadence.
            return 4L;
        }
        FireMode fireMode = gun.getFireMode(gunStack);
        if (fireMode == FireMode.BURST) {
            BurstData burst = gunData.getBurstData();
            if (burst != null && burst.getCount() > 0) {
                // We do not run TACZ's burst state machine, so fire at the sustained
                // rate of a whole burst instead of its intra-burst gap.
                intervalMs = Math.max(intervalMs,
                        Math.max(50L, gunData.getBurstShootInterval()) / burst.getCount());
            }
        }
        boolean tierMagazine = AutoFireTier.of(gunStack) != null;
        if (!tierMagazine && gunData.getBolt() != null) {
            // A player cycle is boltFeedTime followed by boltActionTime, and TACZ
            // rejects shots while the bolt is moving; a pack that leaves
            // boltFeedTime at -1 reuses the action time.
            float action = Math.max(0.0F, gunData.getBoltActionTime());
            float feed = gunData.getBoltFeedTime() < 0.0F ? action : Math.max(0.0F, gunData.getBoltFeedTime());
            intervalMs = Math.max(intervalMs, (long) ((action + feed) * 1000.0F));
        }
        if (!tierMagazine) {
            ChargeData charge = gunData.getChargeData(fireMode);
            if (charge != null && charge.getIncreasePerTick() > 0.0F) {
                float needed = charge.getFireThreshold() > 0.0F ? charge.getFireThreshold() : charge.getMaxCharge();
                intervalMs = Math.max(intervalMs, (long) (Math.ceil(needed / charge.getIncreasePerTick()) * 50.0F));
            }
        }
        // Round up: the halo may fire slower than the gun, never faster.
        long ticks = (intervalMs + 49L) / 50L;
        return Math.max(MIN_INTERVAL_TICKS, ticks);
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
