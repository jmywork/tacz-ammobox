package com.taczammobox.item;

import com.tacz.guns.api.item.IGun;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Easter-egg rack: holds up to ten TACZ guns that orbit the wearer and fight
 * on their own; a marked target pulls every gun onto it. The guns live in the stack's NBT so the rack can be
 * moved between the back slot and the inventory without losing them.
 */
public class SatelliteRackItem extends Item {
    public static final int SLOTS = 10;
    private static final String TAG_GUNS = "Guns";
    private static final String TAG_FOCUS = "Focus";
    private static final String TAG_AMMO = "Ammo";

    private final boolean upkeep;

    public SatelliteRackItem(Properties properties) {
        this(properties, false);
    }

    public SatelliteRackItem(Properties properties, boolean upkeep) {
        super(properties);
        this.upkeep = upkeep;
    }

    /** Upkeep racks feed the halo from real ammo; the easter one never runs dry. */
    public boolean isUpkeep() {
        return upkeep;
    }

    public static boolean consumesAmmo(ItemStack stack) {
        return stack.getItem() instanceof SatelliteRackItem rack && rack.upkeep;
    }

    /** Rounds of one ammo id kept in the rack's own cache. */
    public static int cachedRounds(ItemStack rack, ResourceLocation ammoId) {
        CompoundTag tag = rack.getTag();
        if (tag == null || !tag.contains(TAG_AMMO, Tag.TAG_COMPOUND)) {
            return 0;
        }
        return tag.getCompound(TAG_AMMO).getInt(ammoId.toString());
    }

    public static void addCachedRounds(ItemStack rack, ResourceLocation ammoId, int delta) {
        if (delta == 0) {
            return;
        }
        int next = Math.max(0, cachedRounds(rack, ammoId) + delta);
        CompoundTag tag = rack.getOrCreateTag();
        CompoundTag ammo = tag.getCompound(TAG_AMMO);
        if (next == 0) {
            ammo.remove(ammoId.toString());
        } else {
            ammo.putInt(ammoId.toString(), next);
        }
        tag.put(TAG_AMMO, ammo);
    }

    public static int cachedTypes(ItemStack rack) {
        CompoundTag tag = rack.getTag();
        if (tag == null || !tag.contains(TAG_AMMO, Tag.TAG_COMPOUND)) {
            return 0;
        }
        return tag.getCompound(TAG_AMMO).getAllKeys().size();
    }

    public static List<ItemStack> guns(ItemStack rack) {
        List<ItemStack> out = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            out.add(ItemStack.EMPTY);
        }
        CompoundTag tag = rack.getTag();
        if (tag == null) {
            return out;
        }
        ListTag list = tag.getList(TAG_GUNS, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(SLOTS, list.size()); i++) {
            out.set(i, ItemStack.of(list.getCompound(i)));
        }
        return out;
    }

    public static ItemStack gun(ItemStack rack, int slot) {
        if (slot < 0 || slot >= SLOTS) {
            return ItemStack.EMPTY;
        }
        return guns(rack).get(slot);
    }

    public static void setGun(ItemStack rack, int slot, ItemStack gun) {
        if (slot < 0 || slot >= SLOTS) {
            return;
        }
        List<ItemStack> guns = guns(rack);
        guns.set(slot, gun.isEmpty() ? ItemStack.EMPTY : gun.copy());
        ListTag list = new ListTag();
        for (ItemStack stack : guns) {
            list.add(stack.save(new CompoundTag()));
        }
        rack.getOrCreateTag().put(TAG_GUNS, list);
    }

    public static int loadedCount(ItemStack rack) {
        int count = 0;
        for (ItemStack stack : guns(rack)) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** Guns in slot order, empty slots removed. */
    public static List<ItemStack> loadedGuns(ItemStack rack) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : guns(rack)) {
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        return out;
    }

    /** The marked target, or null when the halo picks its own. */
    @Nullable
    public static UUID focus(ItemStack rack) {
        CompoundTag tag = rack.getTag();
        if (tag == null || !tag.hasUUID(TAG_FOCUS)) {
            return null;
        }
        return tag.getUUID(TAG_FOCUS);
    }

    public static void setFocus(ItemStack rack, @Nullable UUID uuid) {
        if (uuid == null) {
            CompoundTag tag = rack.getTag();
            if (tag != null) {
                tag.remove(TAG_FOCUS);
            }
            return;
        }
        rack.getOrCreateTag().putUUID(TAG_FOCUS, uuid);
    }

    public static boolean isGun(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IGun;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, ignored) -> new com.taczammobox.menu.SatelliteRackMenu(id, inventory),
                    Component.translatable("container.taczammobox.satellite_rack")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(upkeep
                        ? "item.taczammobox.satellite_rack_upkeep.desc"
                        : "item.taczammobox.satellite_rack.desc")
                .withStyle(ChatFormatting.GRAY));
        int loaded = loadedCount(stack);
        if (loaded > 0) {
            tooltip.add(Component.translatable("item.taczammobox.satellite_rack.loaded", loaded, SLOTS)
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("item.taczammobox.satellite_rack.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (upkeep) {
            int cache = cachedTypes(stack);
            tooltip.add(Component.translatable("item.taczammobox.satellite_rack.cache", cache)
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable(upkeep
                        ? "item.taczammobox.satellite_rack_upkeep.hint"
                        : "item.taczammobox.satellite_rack.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
