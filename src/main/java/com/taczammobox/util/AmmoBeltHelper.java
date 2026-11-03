package com.taczammobox.util;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.compat.CuriosCompat;
import com.taczammobox.config.WasteSettings;
import com.taczammobox.item.AmmoBeltItem;
import com.taczammobox.item.PackagedAmmo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.ArrayList;
import java.util.List;

/** Server-side belt feeding for the main-hand TACZ gun. */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID)
public final class AmmoBeltHelper {
    private AmmoBeltHelper() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || event.player.isSpectator()) {
            return;
        }
        if (!com.taczammobox.config.FeatureFlags.belt()) {
            return;
        }
        feed(event.player);
    }

    private static void feed(Player player) {
        ItemStack gunStack = player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return;
        }
        ItemStack belt = findBelt(player);
        if (belt.isEmpty()) {
            return;
        }
        if (gun.hasHeatData(gunStack) && gun.isOverheatLocked(gunStack)) {
            return;
        }
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack)).orElse(null);
        if (index == null) {
            return;
        }
        GunData data = index.getGunData();
        ResourceLocation ammoId = data.getAmmoId();
        // Tier magazines hold more than the gun's stock ammo_amount; the belt must fill up
        // to the real capacity or it would stop feeding once the stock number is reached.
        int magazine = data.getAmmoAmount();
        AutoFireTier tier = AutoFireTier.of(gunStack);
        if (tier != null && data.getBolt() == Bolt.MANUAL_ACTION) {
            magazine = tier.capacity();
        }
        if (ammoId == null || magazine <= 0) {
            return;
        }
        int current = gun.getCurrentAmmoCount(gunStack);
        if (current >= magazine) {
            return;
        }

        ResourceLocation storedId = AmmoBeltItem.storedAmmoId(belt);
        int stored = AmmoBeltItem.storedRounds(belt);
        if (stored > 0 && storedId != null && !storedId.equals(ammoId)) {
            // The belt only carries one ammo type at a time.
            return;
        }
        if (stored <= 0) {
            int cap = WasteSettings.beltMaxStore();
            if (cap > 0 && stored >= cap) {
                return;
            }
            int loaded = consumeAmmo(player, ammoId);
            if (loaded <= 0) {
                return;
            }
            stored = loaded;
            int maxStore = WasteSettings.beltMaxStore();
            if (maxStore > 0 && stored > maxStore) {
                stored = maxStore;
            }
            AmmoBeltItem.store(belt, ammoId, stored);
        }

        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        ReloadState reload = operator.getSynReloadState();
        if (reload.getStateType().isReloading()) {
            if (reload.getStateType().isReloadingEmpty()) {
                operator.cancelReload();
            } else {
                return;
            }
        }

        int toFeed = Math.min(magazine - current, stored);
        if (toFeed <= 0) {
            return;
        }
        gun.setCurrentAmmoCount(gunStack, current + toFeed);
        gun.setBulletInBarrel(gunStack, true);
        AmmoBeltItem.store(belt, ammoId, stored - toFeed);
        if (stored - toFeed <= 0) {
            AmmoBeltItem.clearStore(belt);
        }
        player.getInventory().setChanged();
        if (player.containerMenu != null) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static ItemStack findBelt(Player player) {
        for (IItemHandler handler : handlers(player)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.getItem() instanceof AmmoBeltItem) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static int consumeAmmo(Player player, ResourceLocation ammoId) {
        for (IItemHandler handler : handlers(player)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                // Our own compressed box/crate.
                if (stack.getItem() instanceof PackagedAmmo pkg) {
                    if (!pkg.getType().ammoId().equals(ammoId)) {
                        continue;
                    }
                    int rounds = AmmoBeltItem.roundsPerItem(pkg.getType(), pkg.boxesPerItem());
                    if (!handler.extractItem(slot, 1, false).isEmpty()) {
                        return rounds;
                    }
                    continue;
                }
                // TACZ native ammo box, including the normal 180/360/540 round boxes.
                if (stack.getItem() instanceof IAmmoBox box) {
                    if (box.isCreative(stack) || box.isAllTypeCreative(stack)
                            || !ammoId.equals(box.getAmmoId(stack))) {
                        continue;
                    }
                    int rounds = box.getAmmoCount(stack);
                    if (rounds <= 0) {
                        continue;
                    }
                    if (!handler.extractItem(slot, 1, false).isEmpty()) {
                        return rounds;
                    }
                    continue;
                }
                // Loose TACZ ammo: one item is one round.
                if (stack.getItem() instanceof IAmmo ammo && ammoId.equals(ammo.getAmmoId(stack))) {
                    int count = stack.getCount();
                    if (!handler.extractItem(slot, count, false).isEmpty()) {
                        return count;
                    }
                }
            }
        }
        return 0;
    }

    private static List<IItemHandler> handlers(Player player) {
        List<IItemHandler> handlers = new ArrayList<>();
        handlers.add(new InvWrapper(player.getInventory()));
        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.addEquipped(player, handlers);
        }
        return handlers;
    }
}
