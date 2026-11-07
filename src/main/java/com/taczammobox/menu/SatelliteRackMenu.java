package com.taczammobox.menu;

import com.taczammobox.item.SatelliteRackItem;
import com.taczammobox.registry.ModMenus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Ten gun slots backed directly by the rack stack's NBT. There is no separate
 * inventory object on the server: every {@link #setItem} writes through.
 */
public class SatelliteRackMenu extends AbstractContainerMenu {
    private final ItemStack rack;

    public SatelliteRackMenu(int id, Inventory playerInventory) {
        super(ModMenus.SATELLITE_RACK.get(), id);
        ItemStack main = playerInventory.player.getMainHandItem();
        ItemStack off = playerInventory.player.getOffhandItem();
        if (main.getItem() instanceof SatelliteRackItem) {
            this.rack = main;
        } else if (off.getItem() instanceof SatelliteRackItem) {
            this.rack = off;
        } else {
            this.rack = ItemStack.EMPTY;
        }

        Container container = new RackContainer(this.rack);
        for (int i = 0; i < SatelliteRackItem.SLOTS; i++) {
            int col = i % 5;
            int row = i / 5;
            this.addSlot(new Slot(container, i, 44 + col * 20, 22 + row * 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return SatelliteRackItem.isGun(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 88 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 146));
        }
    }

    public ItemStack rack() {
        return this.rack;
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.rack.isEmpty() && (player.getMainHandItem() == this.rack
                || player.getOffhandItem() == this.rack || player.getInventory().contains(this.rack));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        copy = stack.copy();
        int rackSlots = SatelliteRackItem.SLOTS;
        if (index < rackSlots) {
            if (!this.moveItemStackTo(stack, rackSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (SatelliteRackItem.isGun(stack)) {
            if (!this.moveItemStackTo(stack, 0, rackSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    /** ItemStack-backed container: 10 NBT entries, written on every change. */
    private static final class RackContainer implements Container {
        private static final String TAG_GUNS = "Guns";
        private final ItemStack rack;

        private RackContainer(ItemStack rack) {
            this.rack = rack;
        }

        @Override
        public int getContainerSize() {
            return SatelliteRackItem.SLOTS;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < getContainerSize(); i++) {
                if (!getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return SatelliteRackItem.gun(this.rack, slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                setItem(slot, ItemStack.EMPTY);
            }
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = getItem(slot);
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            SatelliteRackItem.setGun(this.rack, slot, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            // NBT write happens in setItem; nothing to flush.
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            ListTag empty = new ListTag();
            for (int i = 0; i < SatelliteRackItem.SLOTS; i++) {
                empty.add(ItemStack.EMPTY.save(new CompoundTag()));
            }
            this.rack.getOrCreateTag().put(TAG_GUNS, empty);
        }
    }
}
