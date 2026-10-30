package com.taczammobox.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class DynamicCrateBlockEntity extends BlockEntity {
    @Nullable
    private ResourceLocation ammoId;

    public DynamicCrateBlockEntity(BlockPos pos, BlockState state) {
        super(com.taczammobox.registry.ModBlockEntities.DYNAMIC_CRATE.get(), pos, state);
    }

    @Nullable
    public ResourceLocation getAmmoId() {
        return ammoId;
    }

    public void setAmmoId(@Nullable ResourceLocation ammoId) {
        this.ammoId = ammoId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ammoId != null) {
            tag.putString("AmmoId", ammoId.toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ammoId = tag.contains("AmmoId") ? ResourceLocation.tryParse(tag.getString("AmmoId")) : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
