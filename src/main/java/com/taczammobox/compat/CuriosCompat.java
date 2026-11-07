package com.taczammobox.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.function.Predicate;

/** Only loaded when Curios is installed. */
public final class CuriosCompat {
    private CuriosCompat() {
    }

    /** First equipped curio matching the filter, or an empty stack. */
    public static ItemStack findCurio(Player player, Predicate<ItemStack> filter) {
        ItemStack[] found = {ItemStack.EMPTY};
        CuriosApi.getCuriosInventory(player).ifPresent(curios -> {
            IItemHandler equipped = curios.getEquippedCurios();
            if (equipped == null) {
                return;
            }
            for (int i = 0; i < equipped.getSlots(); i++) {
                ItemStack stack = equipped.getStackInSlot(i);
                if (!stack.isEmpty() && filter.test(stack)) {
                    found[0] = stack;
                    return;
                }
            }
        });
        return found[0];
    }

    public static void addEquipped(Player player, List<IItemHandler> handlers) {
        CuriosApi.getCuriosInventory(player).ifPresent(curios -> {
            IItemHandler equipped = curios.getEquippedCurios();
            if (equipped != null) {
                handlers.add(equipped);
            }
        });
    }
}
