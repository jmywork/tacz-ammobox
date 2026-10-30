package com.taczammobox.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * Exposes the non-player slots of the currently open menu as an item handler.
 * This lets the backpack GUI's own slots be used as ammo-box targets/sources
 * without relying on the backpack item's capability being reachable.
 */
public final class ContainerSlotHandler implements IItemHandlerModifiable {
    private final List<Slot> slots = new ArrayList<>();

    public ContainerSlotHandler(AbstractContainerMenu menu, Player player) {
        for (Slot slot : menu.slots) {
            if (slot instanceof ResultSlot) {
                continue;
            }
            if (slot.container == player.getInventory()) {
                continue;
            }
            slots.add(slot);
        }
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    @Override
    public int getSlots() {
        return slots.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < slots.size() ? slots.get(slot).getItem() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= slots.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        Slot target = slots.get(slot);
        ItemStack stack = target.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int count = Math.min(amount, Math.min(stack.getCount(), target.getMaxStackSize()));
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.copyWithCount(count);
        if (!simulate) {
            stack.shrink(count);
            target.setChanged();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot >= 0 && slot < slots.size() ? slots.get(slot).getMaxStackSize() : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < slots.size() && slots.get(slot).mayPlace(stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < slots.size()) {
            slots.get(slot).set(stack);
            slots.get(slot).setChanged();
        }
    }
}
