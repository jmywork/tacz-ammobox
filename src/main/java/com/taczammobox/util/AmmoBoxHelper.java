package com.taczammobox.util;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.taczammobox.AmmoType;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.compat.CuriosCompat;
import com.taczammobox.compat.DCSharpCompat;
import com.taczammobox.config.WasteSettings;
import com.taczammobox.item.PackagedAmmo;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AmmoBoxHelper {
    public static final int BOXES_PER_CRATE = 9;
    private static final int FALLBACK_ROUNDS = 60;
    private static final Map<String, Integer> CAPACITY_CACHE = new HashMap<>();

    private AmmoBoxHelper() {
    }

    /**
     * One box holds exactly one stack of the ammo type, using TACZ's own
     * {@code stack_size} (gun packs are free to change it per ammo).
     */
    public static int roundsPerBox(AmmoType type) {
        return TimelessAPI.getCommonAmmoIndex(type.ammoId())
                .map(CommonAmmoIndex::getStackSize)
                .orElse(FALLBACK_ROUNDS);
    }

    public static boolean unbox(Level level, Player player, ItemStack boxStack, AmmoType type, int boxes) {
        if (level.isClientSide) {
            return true;
        }
        if (boxStack.getCount() < boxes) {
            return false;
        }
        ItemStack ammo = AmmoItemBuilder.create()
                .setId(type.ammoId())
                .setCount(roundsPerBox(type) * boxes)
                .build();
        boxStack.shrink(boxes);
        ItemHandlerHelper.giveItemToPlayer(player, ammo);
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);
        return true;
    }

    public static boolean unboxBoxes(Level level, Player player, ItemStack crateStack, AmmoType type, int boxes) {
        if (level.isClientSide) {
            return true;
        }
        if (crateStack.isEmpty()) {
            return false;
        }
        ItemStack boxStack = new ItemStack(ModItems.box(type.ammoId()), boxes);
        crateStack.shrink(1);
        ItemHandlerHelper.giveItemToPlayer(player, boxStack);
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);
        return true;
    }

    public static boolean fillAmmoBox(Level level, Player player, ItemStack source, AmmoType type, int boxes) {
        if (level.isClientSide) {
            return true;
        }
        if (source.isEmpty()) {
            return false;
        }
        ItemStack refillTemplate = source.copy();
        refillTemplate.setCount(1);
        boolean fromHand = source == player.getMainHandItem();
        int rounds = roundsPerBox(type) * boxes;
        boolean waste = WasteSettings.waste();

        for (IItemHandler inventory : ammoBoxHandlers(player)) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack candidate = inventory.getStackInSlot(i);
                if (!(candidate.getItem() instanceof IAmmoBox ammoBox)) {
                    continue;
                }
                if (ammoBox.isCreative(candidate) || ammoBox.isAllTypeCreative(candidate)) {
                    continue;
                }
                ResourceLocation boxAmmoId = ammoBox.getAmmoId(candidate);
                boolean empty = boxAmmoId.equals(DefaultAssets.EMPTY_AMMO_ID);
                if (!empty && !boxAmmoId.equals(type.ammoId())) {
                    continue;
                }
                int capacity = capacity(ammoBox, candidate, type);
                int current = ammoBox.getAmmoCount(candidate);
                int[] space = {capacity - current};
                if (space[0] <= 0) {
                    continue;
                }

                int added = 0;
                while (space[0] > 0 && !source.isEmpty()) {
                    int toAdd = packageAmount(space[0], rounds, waste);
                    if (toAdd <= 0) {
                        break;
                    }
                    source.shrink(1);
                    added += toAdd;
                    space[0] -= toAdd;
                    if (waste && toAdd < rounds) {
                        break;
                    }
                }
                if (space[0] > 0) {
                    added += consumePackages(player, refillTemplate, rounds, space, waste);
                }
                if (added <= 0) {
                    continue;
                }

                ammoBox.setAmmoId(candidate, type.ammoId());
                ammoBox.setAmmoCount(candidate, current + added);
                writeBack(inventory, i, candidate);
                if (fromHand && source.isEmpty()) {
                    refillHand(player, refillTemplate);
                }
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);
                return true;
            }
        }
        player.displayClientMessage(Component.translatable("message.taczammobox.no_space"), true);
        return false;
    }

    private static int packageAmount(int space, int rounds, boolean waste) {
        if (waste) {
            return Math.min(space, rounds);
        }
        return space >= rounds ? rounds : 0;
    }

    /** Consume matching packages from the player's inventory/Curios/nested backpacks. */
    private static int consumePackages(Player player, ItemStack template, int rounds, int[] space, boolean waste) {
        int added = 0;
        outer:
        for (IItemHandler inventory : ammoBoxHandlers(player)) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                while (space[0] > 0) {
                    ItemStack candidate = inventory.getStackInSlot(i);
                    if (!samePackage(candidate, template)) {
                        break;
                    }
                    int toAdd = packageAmount(space[0], rounds, waste);
                    if (toAdd <= 0) {
                        break outer;
                    }
                    ItemStack extracted = inventory.extractItem(i, 1, false);
                    if (extracted.isEmpty()) {
                        break;
                    }
                    added += toAdd;
                    space[0] -= toAdd;
                    if (waste && toAdd < rounds) {
                        break outer;
                    }
                }
            }
        }
        return added;
    }

    private static boolean samePackage(ItemStack candidate, ItemStack template) {
        if (!(candidate.getItem() instanceof PackagedAmmo candidatePackage)
                || !(template.getItem() instanceof PackagedAmmo templatePackage)) {
            return false;
        }
        return candidatePackage.boxesPerItem() == templatePackage.boxesPerItem()
                && candidatePackage.getType().ammoId().equals(templatePackage.getType().ammoId())
                && ItemStack.isSameItemSameTags(candidate, template);
    }

    /**
     * Fill a matching ammo box from a held stack of loose TACZ ammo. Unlike a
     * package this never "wastes": it transfers as much as the box can hold and
     * leaves the remainder in the player's hand.
     */
    public static boolean fillFromLooseAmmo(Level level, Player player, ItemStack ammoStack) {
        if (level.isClientSide) {
            return true;
        }
        if (!(ammoStack.getItem() instanceof IAmmo ammoItem) || ammoStack.isEmpty()) {
            return false;
        }
        ResourceLocation ammoId = ammoItem.getAmmoId(ammoStack);
        AmmoType type = AmmoTypes.byId(ammoId);
        if (type == null) {
            return false;
        }
        ItemStack refillTemplate = ammoStack.copy();
        refillTemplate.setCount(1);
        boolean fromHand = ammoStack == player.getMainHandItem();

        for (IItemHandler inventory : ammoBoxHandlers(player)) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack candidate = inventory.getStackInSlot(i);
                if (!(candidate.getItem() instanceof IAmmoBox ammoBox)) {
                    continue;
                }
                if (ammoBox.isCreative(candidate) || ammoBox.isAllTypeCreative(candidate)) {
                    continue;
                }
                ResourceLocation boxAmmoId = ammoBox.getAmmoId(candidate);
                boolean empty = boxAmmoId.equals(DefaultAssets.EMPTY_AMMO_ID);
                if (!empty && !boxAmmoId.equals(type.ammoId())) {
                    continue;
                }
                int space = capacity(ammoBox, candidate, type) - ammoBox.getAmmoCount(candidate);
                if (space <= 0) {
                    continue;
                }
                int added = Math.min(space, ammoStack.getCount());
                if (added <= 0) {
                    continue;
                }
                if (!player.isCreative()) {
                    ammoStack.shrink(added);
                    int remainingSpace = space - added;
                    if (remainingSpace > 0) {
                        added += consumeOtherLooseAmmo(player, type, remainingSpace);
                    }
                }
                ammoBox.setAmmoId(candidate, type.ammoId());
                ammoBox.setAmmoCount(candidate, ammoBox.getAmmoCount(candidate) + added);
                writeBack(inventory, i, candidate);
                if (!player.isCreative() && fromHand && ammoStack.isEmpty()) {
                    refillHand(player, refillTemplate);
                }
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);
                return true;
            }
        }
        player.displayClientMessage(Component.translatable("message.taczammobox.no_space"), true);
        return false;
    }

    /** Loose ammo is never wasted: keep pulling matching stacks until the box is full or they run out. */
    private static int consumeOtherLooseAmmo(Player player, AmmoType type, int needed) {
        int moved = 0;
        for (IItemHandler handler : ammoBoxHandlers(player)) {
            for (int slot = 0; slot < handler.getSlots() && needed > 0; slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!(stack.getItem() instanceof IAmmo ammo) || stack.isEmpty()) {
                    continue;
                }
                if (!ammo.getAmmoId(stack).equals(type.ammoId())) {
                    continue;
                }
                int take = Math.min(needed, stack.getCount());
                ItemStack extracted = handler.extractItem(slot, take, false);
                if (extracted.isEmpty()) {
                    continue;
                }
                moved += extracted.getCount();
                needed -= extracted.getCount();
            }
            if (needed <= 0) {
                break;
            }
        }
        return moved;
    }

    private static List<IItemHandler> ammoBoxHandlers(Player player) {
        List<IItemHandler> handlers = new ArrayList<>();
        // Use the actual player inventory directly. Some capability wrappers
        // return copies, which makes writes to a box in the main inventory/hand
        // silently disappear.
        IItemHandler main = new InvWrapper(player.getInventory());
        handlers.add(main);

        List<IItemHandler> curiosHandlers = new ArrayList<>();
        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.addEquipped(player, curiosHandlers);
            for (IItemHandler curios : curiosHandlers) {
                if (!handlers.contains(curios)) {
                    handlers.add(curios);
                }
            }
        }

        boolean containerOpen = player.containerMenu != null && player.containerMenu != player.inventoryMenu;
        if (containerOpen) {
            // Only the currently opened container is scanned. Nested backpacks
            // (backpack inside backpack) are intentionally ignored.
            ContainerSlotHandler openContainer = new ContainerSlotHandler(player.containerMenu, player);
            if (!openContainer.isEmpty()) {
                handlers.add(openContainer);
            }
        }
        return handlers;
    }

    private static void writeBack(IItemHandler handler, int slot, ItemStack stack) {
        if (handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(slot, stack);
        }
    }

    /**
     * If a fill consumed the last item in the main hand, move one matching stack
     * from the player inventory, Curios or a nested backpack into the hand.
     */
    private static void refillHand(Player player, ItemStack template) {
        if (!WasteSettings.autoRefill()) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || template.isEmpty()
                || !serverPlayer.getMainHandItem().isEmpty()) {
            return;
        }
        Inventory inventory = serverPlayer.getInventory();
        int selected = inventory.selected;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (i == selected || (i >= 36 && i < 40)) {
                continue;
            }
            ItemStack candidate = inventory.getItem(i);
            if (!matches(candidate, template)) {
                continue;
            }
            int move = Math.min(candidate.getCount(), candidate.getMaxStackSize());
            ItemStack moved = candidate.split(move);
            inventory.setItem(i, candidate);
            inventory.setItem(selected, moved);
            inventory.setChanged();
            return;
        }

        List<IItemHandler> extras = new ArrayList<>();
        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.addEquipped(serverPlayer, extras);
        }
        for (IItemHandler handler : extras) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack candidate = handler.getStackInSlot(slot);
                if (!matches(candidate, template)) {
                    continue;
                }
                int move = Math.min(candidate.getCount(), candidate.getMaxStackSize());
                ItemStack extracted = handler.extractItem(slot, move, false);
                if (extracted.isEmpty()) {
                    continue;
                }
                serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, extracted);
                return;
            }
        }
    }

    private static boolean matches(ItemStack candidate, ItemStack template) {
        if (candidate.isEmpty() || template.isEmpty()) {
            return false;
        }
        if (candidate.getItem() instanceof IAmmo candidateAmmo
                && template.getItem() instanceof IAmmo templateAmmo) {
            return candidateAmmo.getAmmoId(candidate).equals(templateAmmo.getAmmoId(template));
        }
        return ItemStack.isSameItemSameTags(candidate, template);
    }

    public static int capacity(IAmmoBox ammoBox, ItemStack stack, AmmoType type) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int level = ammoBox.getAmmoLevel(stack);
        Integer override = WasteSettings.boxCapacity(itemId, level, type.ammoId());
        if (override != null) {
            return override;
        }
        int formula = roundsPerBox(type) * SyncConfig.AMMO_BOX_STACK_SIZE.get() * (level + 1);
        Double multiplier = WasteSettings.boxCapacityMultiplier(itemId, level, type.ammoId());
        if (multiplier != null) {
            return Math.max(1, WasteSettings.roundCapacity(formula * multiplier));
        }
        int dcsharpStacks = DCSharpCompat.stackCountForLevel(level);
        if (dcsharpStacks > 0) {
            return Math.max(1, roundsPerBox(type) * dcsharpStacks);
        }

        String key = itemId + "|" + level + "|" + type.ammoId();
        Integer cached = CAPACITY_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int detected = probeCapacity(ammoBox, stack, formula);
        if (detected != formula && formula > 0) {
            double exactRatio = (double) detected / formula;
            double ratio = findSimpleMultiplier(formula, detected, exactRatio);
            String keyBase = itemId + "#" + level + "#" + type.ammoId();
            if (!Double.isNaN(ratio)) {
                TaczAmmoBoxMod.LOGGER.info(
                        "Detected custom ammo box multiplier {} for {} (formula was {}, detected {})",
                        ratio, key, formula, detected);
                if (isServerThread()) {
                    WasteSettings.recordDetectedMultiplier(itemId, ratio);
                }
            } else {
                // The capacity is not a clean multiple of the formula (additive or
                // mixed change); store the exact absolute value instead.
                TaczAmmoBoxMod.LOGGER.info(
                        "Detected custom ammo box capacity {} for {} (formula was {}, not a clean multiplier)",
                        detected, key, formula);
                if (isServerThread()) {
                    WasteSettings.recordDetectedCapacity(keyBase, detected);
                }
            }
        }
        CAPACITY_CACHE.put(key, detected);
        return detected;
    }

    /** Find a short decimal multiplier that reproduces the detected capacity. */
    private static double findSimpleMultiplier(int formula, int detected, double exact) {
        double twoDecimals = findMultiplier(formula, detected, exact, 100.0, 200);
        if (!Double.isNaN(twoDecimals)) {
            return twoDecimals;
        }
        return findMultiplier(formula, detected, exact, 1000.0, 150);
    }

    private static double findMultiplier(int formula, int detected, double exact, double scale, int radius) {
        int center = (int) Math.round(exact * scale);
        for (int delta = 0; delta <= radius; delta++) {
            for (int sign : new int[]{1, -1}) {
                int scaled = center + sign * delta;
                if (scaled <= 0) {
                    continue;
                }
                double candidate = scaled / scale;
                if (WasteSettings.roundCapacity(formula * candidate) == detected) {
                    return candidate;
                }
            }
        }
        return Double.NaN;
    }

    private static boolean isServerThread() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread();
    }

    public static void clearCapacityCache() {
        CAPACITY_CACHE.clear();
    }

    /**
     * TACZ's IAmmoBox has no max-capacity method. A box implementation that
     * clamps setAmmoCount can still be queried safely on a copy; if it does not
     * clamp, fall back to TACZ's formula.
     */
    private static int probeCapacity(IAmmoBox ammoBox, ItemStack stack, int formula) {
        try {
            ItemStack probe = stack.copy();
            ammoBox.setAmmoCount(probe, 1_000_000);
            int read = ammoBox.getAmmoCount(probe);
            if (read > 0 && read < 1_000_000) {
                return read;
            }
        } catch (RuntimeException exception) {
            TaczAmmoBoxMod.LOGGER.debug("Could not probe ammo box capacity, using formula", exception);
        }
        return formula;
    }
}
