package com.taczammobox.menu;

import com.tacz.guns.api.item.IAmmo;
import com.taczammobox.block.AmmoLoaderBlockEntity;
import com.taczammobox.item.PackagedAmmo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class AmmoLoaderMenu extends AbstractContainerMenu {
    private final AmmoLoaderBlockEntity loader;
    private final ContainerData data;

    public AmmoLoaderMenu(int id, Inventory inventory, AmmoLoaderBlockEntity loader) {
        super(com.taczammobox.registry.ModMenus.LOADER.get(), id);
        this.loader = loader;
        this.data = loader.getData();
        addDataSlots(data);

        ItemStackHandler handler = loader.getInventory();
        addSlot(new SlotItemHandler(handler, AmmoLoaderBlockEntity.SLOT_BOX_IN, 44, 35));
        addSlot(new SlotItemHandler(handler, AmmoLoaderBlockEntity.SLOT_SOURCE, 80, 35));
        addSlot(new SlotItemHandler(handler, AmmoLoaderBlockEntity.SLOT_BOX_OUT, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public AmmoLoaderMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (AmmoLoaderBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }

    public int getProgress() {
        return data.get(0);
    }

    public AmmoLoaderBlockEntity.SideMode getSide(int index) {
        return AmmoLoaderBlockEntity.SideMode.values()[data.get(1 + index)];
    }

    public AmmoLoaderBlockEntity.SideMode getSide(Direction direction) {
        return getSide(direction.ordinal());
    }

    public AmmoLoaderBlockEntity.RedstoneMode getRedstoneMode() {
        return AmmoLoaderBlockEntity.RedstoneMode.values()[data.get(7)];
    }

    public Direction getFacing() {
        return loader.getBlockState().getValue(com.taczammobox.block.AmmoLoaderBlock.FACING);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 1 && id <= 6) {
            loader.cycleSide(id - 1);
            return true;
        }
        if (id == 7) {
            loader.cycleRedstone();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stack.getItem() instanceof com.tacz.guns.api.item.IAmmoBox) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof IAmmo || stack.getItem() instanceof PackagedAmmo) {
                if (!moveItemStackTo(stack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockPos pos = loader.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
