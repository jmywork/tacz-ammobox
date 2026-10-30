package com.taczammobox.util;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/** Batched auto-I/O: one simulated insert per source slot instead of 16 single-item moves. */
public final class AutoIoHelper {
    private AutoIoHelper() {
    }

    public static void pull(IItemHandler target, IItemHandler source) {
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack available = source.extractItem(slot, source.getStackInSlot(slot).getCount(), true);
            if (available.isEmpty()) {
                continue;
            }
            ItemStack simulated = ItemHandlerHelper.insertItem(target, available, true);
            int move = available.getCount() - simulated.getCount();
            if (move <= 0) {
                continue;
            }
            ItemStack taken = source.extractItem(slot, move, false);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack leftover = ItemHandlerHelper.insertItem(target, taken, false);
            if (!leftover.isEmpty()) {
                source.insertItem(slot, leftover, false);
            }
        }
    }
}
