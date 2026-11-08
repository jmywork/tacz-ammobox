package com.taczammobox.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
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

    public void setNextFire(long nextFire) {
        this.nextFire = nextFire;
    }
}
