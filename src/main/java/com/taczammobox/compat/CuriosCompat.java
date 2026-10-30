package com.taczammobox.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/** Only loaded when Curios is installed. */
public final class CuriosCompat {
    private CuriosCompat() {
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
