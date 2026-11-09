package com.taczammobox.util;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.compat.CuriosCompat;
import com.taczammobox.config.WasteSettings;
import com.taczammobox.item.AmmoBeltItem;
import com.taczammobox.item.PackagedAmmo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feed belt behaviour: turns our boxed/crated ammo into gun reloads.
 *
 * <p>The reload itself stays vanilla TACZ — only the ammo source changes. When a reload
 * finishes, the belt tops the magazine up to its capacity from its own reserve, pulling a
 * fresh box/crate out of the inventory (or a top-level Sophisticated Backpack) when the
 * reserve runs dry. Leftovers stay in the belt NBT.</p>
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID)
public final class AmmoChainHelper {
    private static final Map<UUID, Boolean> WAS_RELOADING = new ConcurrentHashMap<>();

    private AmmoChainHelper() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        if (!com.taczammobox.config.FeatureFlags.ammoChain()) {
            return;
        }
        boolean reloading = isReloading(player);
        Boolean previous = WAS_RELOADING.get(player.getUUID());
        // Only touch the map on a state change; this runs for every player every tick.
        if (previous == null || previous != reloading) {
            WAS_RELOADING.put(player.getUUID(), reloading);
        }
        if (previous != null && previous && !reloading) {
            onReloadFinished(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        WAS_RELOADING.remove(event.getEntity().getUUID());
    }

    private static boolean isReloading(Player player) {
        try {
            return IGunOperator.fromLivingEntity(player).getSynReloadState().getStateType().isReloading();
        } catch (Throwable throwable) {
            return false;
        }
    }

    /** Called once when the reload state drops back to idle. */
    private static void onReloadFinished(Player player) {
        ItemStack gunStack = player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return;
        }
        ItemStack chain = findChain(player);
        if (chain.isEmpty()) {
            return;
        }
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack)).orElse(null);
        if (index == null) {
            return;
        }
        GunData data = index.getGunData();
        ResourceLocation ammoId = data.getAmmoId();
        if (ammoId == null) {
            return;
        }
        int capacity = AttachmentDataUtils.getAmmoCountWithAttachment(gunStack, data);
        int current = gun.getCurrentAmmoCount(gunStack);
        if (current >= capacity) {
            return;
        }
        int reserve = roundsIn(chain, ammoId);
        while (current < capacity) {
            if (reserve <= 0) {
                reserve = loadReserve(player, chain, ammoId);
                if (reserve <= 0) {
                    break;
                }
            }
            int take = Math.min(capacity - current, reserve);
            current += take;
            reserve -= take;
        }
        AmmoBeltItem.store(chain, ammoId, reserve);
        gun.setCurrentAmmoCount(gunStack, current);
        gun.setBulletInBarrel(gunStack, true);
        player.getInventory().setChanged();
        if (player.containerMenu != null) {
            player.containerMenu.broadcastChanges();
        }
    }

    public static boolean hasReloadSource(Player player, ItemStack gunStack) {
        if (!com.taczammobox.config.FeatureFlags.ammoChain()) {
            return false;
        }
        ItemStack chain = findChain(player);
        if (chain.isEmpty()) {
            return false;
        }
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return false;
        }
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack)).orElse(null);
        if (index == null || index.getGunData().getAmmoId() == null) {
            return false;
        }
        ResourceLocation ammoId = index.getGunData().getAmmoId();
        if (roundsIn(chain, ammoId) > 0) {
            return true;
        }
        return hasAnySource(player, ammoId);
    }

    private static int roundsIn(ItemStack chain, ResourceLocation ammoId) {
        ResourceLocation stored = AmmoBeltItem.storedAmmoId(chain);
        if (stored != null && !stored.equals(ammoId)) {
            return 0;
        }
        return AmmoBeltItem.storedRounds(chain);
    }

    /**
     * Pulls one ammo source out of the inventory and returns its round count.
     * Priority: our box/crate, then a TACZ ammo box, then loose TACZ ammo.
     */
    private static int loadReserve(Player player, ItemStack chain, ResourceLocation ammoId) {
        int cap = WasteSettings.beltMaxStore();
        if (cap > 0 && AmmoBeltItem.storedRounds(chain) >= cap) {
            return 0;
        }
        for (IItemHandler handler : handlers(player)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty() || !(stack.getItem() instanceof PackagedAmmo pkg)) {
                    continue;
                }
                if (!pkg.getType().ammoId().equals(ammoId)) {
                    continue;
                }
                int rounds = AmmoBeltItem.roundsPerItem(pkg.getType(), pkg.boxesPerItem());
                if (!handler.extractItem(slot, 1, false).isEmpty()) {
                    return rounds;
                }
            }
        }
        for (IItemHandler handler : handlers(player)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!(stack.getItem() instanceof IAmmoBox box)) {
                    continue;
                }
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
            }
        }
        for (IItemHandler handler : handlers(player)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!(stack.getItem() instanceof IAmmo ammo) || !ammoId.equals(ammo.getAmmoId(stack))) {
                    continue;
                }
                int rounds = stack.getCount();
                if (!handler.extractItem(slot, rounds, false).isEmpty()) {
                    return rounds;
                }
            }
        }
        return 0;
    }

    /** Cheap availability check for the reload gate. */
    private static boolean hasAnySource(Player player, ResourceLocation ammoId) {
        for (IItemHandler handler : handlers(player)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                if (stack.getItem() instanceof PackagedAmmo pkg && pkg.getType().ammoId().equals(ammoId)) {
                    return true;
                }
                if (stack.getItem() instanceof IAmmoBox box && !box.isCreative(stack)
                        && !box.isAllTypeCreative(stack) && ammoId.equals(box.getAmmoId(stack))
                        && box.getAmmoCount(stack) > 0) {
                    return true;
                }
                if (stack.getItem() instanceof IAmmo ammo && ammoId.equals(ammo.getAmmoId(stack))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ItemStack findChain(Player player) {
        for (IItemHandler handler : handlers(player)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.getItem() instanceof com.taczammobox.item.AmmoChainItem) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Player inventory, Curios, plus one level of Sophisticated Backpacks. Nested backpacks
     * are deliberately not descended into.
     */
    private static List<IItemHandler> handlers(Player player) {
        List<IItemHandler> handlers = new ArrayList<>();
        handlers.add(new InvWrapper(player.getInventory()));
        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.addEquipped(player, handlers);
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            stack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                if (handler.getSlots() > 0) {
                    handlers.add(handler);
                }
            });
        }
        return handlers;
    }
}
