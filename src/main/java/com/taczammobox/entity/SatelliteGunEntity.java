package com.taczammobox.entity;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * One floating gun of the easter-egg rack. It never moves by itself: the
 * server-side manager teleports it onto its slot of the halo every tick.
 */
public class SatelliteGunEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> DATA_GUN =
            SynchedEntityData.defineId(SatelliteGunEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_INDEX =
            SynchedEntityData.defineId(SatelliteGunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RECOIL =
            SynchedEntityData.defineId(SatelliteGunEntity.class, EntityDataSerializers.FLOAT);
    private boolean flashSeen;
    private static final EntityDataAccessor<Integer> DATA_FLASH =
            SynchedEntityData.defineId(SatelliteGunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ROLL =
            SynchedEntityData.defineId(SatelliteGunEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID owner;
    private long nextFire;
    private net.minecraft.world.phys.Vec3 aimTarget;
    private long aimUntil;
    private long combatUntil;
    /** Server-side only: the TACZ state holder this gun shoots through. */
    @Nullable
    private FakePlayer shooter;

    // Heat is server-side only: the halo must overheat like the real gun instead of
    // firing forever, otherwise magazine-fed energy guns flood the world.
    private String gunSignature = "";
    private int liveBullets;
    private long bulletCheckTick = Long.MIN_VALUE / 2L;
    private int magazine;
    private float heat;
    private long heatLockUntil;
    private long lastShotTick = Long.MIN_VALUE;
    /** Last cadence that was logged, so one line is printed per gun change. */
    private long loggedInterval = -1L;

    public SatelliteGunEntity(EntityType<? extends SatelliteGunEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_GUN, ItemStack.EMPTY);
        this.entityData.define(DATA_INDEX, 0);
        this.entityData.define(DATA_RECOIL, 0.0F);
        this.entityData.define(DATA_FLASH, 0);
        this.entityData.define(DATA_ROLL, 0.0F);
    }

    @Override
    public void tick() {
        if (this.level().isClientSide()) {
            float recoil = this.getRecoil();
            if (recoil > 0.0F) {
                this.setRecoil(Math.max(0.0F, recoil - 0.25F));
            }
            int flash = this.getFlash();
            if (flash > 0) {
                this.setFlash(flash - 1);
            } else {
                this.flashSeen = false;
            }
        }
        super.tick();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.owner = tag.getUUID("Owner");
        }
        if (tag.contains("Gun")) {
            this.setGun(ItemStack.of(tag.getCompound("Gun")));
        }
        this.setSlotIndex(tag.getInt("Slot"));
        this.setRoll(tag.getFloat("Roll"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.owner != null) {
            tag.putUUID("Owner", this.owner);
        }
        tag.put("Gun", this.getGun().save(new CompoundTag()));
        tag.putInt("Slot", this.getSlotIndex());
        tag.putFloat("Roll", this.getRoll());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Nullable
    public UUID getOwner() {
        return this.owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public ItemStack getGun() {
        return this.entityData.get(DATA_GUN);
    }

    /**
     * The synced gun stack carries the loaded round count, which changes on every
     * upkeep shot. Pushing that through entity data resends the whole stack to every
     * tracking client each shot, so only push when the parts the client draws change.
     */
    public void setGunIfChanged(ItemStack gun) {
        String signature = gunSignature(gun);
        if (signature.equals(this.gunSignature)) {
            return;
        }
        this.gunSignature = signature;
        this.setGun(gun);
    }

    private static String gunSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }
        StringBuilder builder = new StringBuilder(gun.getGunId(stack).toString());
        for (AttachmentType type : AttachmentType.values()) {
            builder.append('|').append(gun.getAttachmentId(stack, type));
        }
        return builder.toString();
    }

    public void setGun(ItemStack gun) {
        this.entityData.set(DATA_GUN, gun == null ? ItemStack.EMPTY : gun);
    }

    public int getSlotIndex() {
        return this.entityData.get(DATA_INDEX);
    }

    public void setSlotIndex(int index) {
        this.entityData.set(DATA_INDEX, index);
    }

    public float getRecoil() {
        return this.entityData.get(DATA_RECOIL);
    }

    public void setRecoil(float recoil) {
        this.entityData.set(DATA_RECOIL, recoil);
    }

    /**
     * True once per shot on the client, so the renderer can kick off TACZ's
     * own muzzle flash exactly once instead of every frame.
     */
    public boolean consumeFlashStart() {
        if (this.flashSeen || this.getFlash() <= 0) {
            return false;
        }
        this.flashSeen = true;
        return true;
    }

    public int getFlash() {
        return this.entityData.get(DATA_FLASH);
    }

    public void setFlash(int ticks) {
        this.entityData.set(DATA_FLASH, ticks);
    }

    public float getRoll() {
        return this.entityData.get(DATA_ROLL);
    }

    public void setRoll(float roll) {
        this.entityData.set(DATA_ROLL, roll);
    }

    public void setAimTarget(net.minecraft.world.phys.Vec3 target, long until) {
        this.aimTarget = target;
        this.aimUntil = until;
    }

    public net.minecraft.world.phys.Vec3 getAimTarget() {
        return this.aimTarget;
    }

    public void setCombatUntil(long until) {
        this.combatUntil = until;
    }

    public long getCombatUntil() {
        return this.combatUntil;
    }

    public long getAimUntil() {
        return this.aimUntil;
    }

    public long getNextFire() {
        return this.nextFire;
    }

    /** Upkeep rack: rounds left in this gun's magazine. */
    public int getLiveBullets() {
        return liveBullets;
    }

    public void setLiveBullets(int liveBullets) {
        this.liveBullets = liveBullets;
    }

    public long getBulletCheckTick() {
        return bulletCheckTick;
    }

    public void setBulletCheckTick(long bulletCheckTick) {
        this.bulletCheckTick = bulletCheckTick;
    }

    public int getMagazine() {
        return magazine;
    }

    public void setMagazine(int magazine) {
        this.magazine = Math.max(0, magazine);
    }

    public float getHeat() {
        return heat;
    }

    public void setHeat(float heat) {
        this.heat = heat;
    }

    public long getHeatLockUntil() {
        return heatLockUntil;
    }

    public void setHeatLockUntil(long tick) {
        this.heatLockUntil = tick;
    }

    public long getLastShotTick() {
        return lastShotTick;
    }

    public void setLastShotTick(long tick) {
        this.lastShotTick = tick;
    }

    /** Last tick interval this gun was logged with, -1 until it has fired once. */
    public long getLoggedInterval() {
        return loggedInterval;
    }

    public void setLoggedInterval(long loggedInterval) {
        this.loggedInterval = loggedInterval;
    }

    public void setNextFire(long nextFire) {
        this.nextFire = nextFire;
    }

    /**
     * TACZ needs a living entity that keeps a persistent shooter state (draw,
     * aim, heat, attachment cache), so every satellite owns one fake player and
     * the manager ticks it along with the gun, the same way the sentry arm does.
     */
    public FakePlayer shooter(ServerLevel level) {
        if (this.shooter == null) {
            UUID id = UUID.nameUUIDFromBytes(("taczammobox:satellite:" + this.getUUID()).getBytes(StandardCharsets.UTF_8));
            this.shooter = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(id, "[Satellite]"));
        }
        return this.shooter;
    }
}
