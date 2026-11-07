package com.taczammobox.item;

import com.tacz.guns.api.item.IGun;
import com.taczammobox.config.FeatureFlags;
import net.minecraft.ChatFormatting;
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

/**
 * Easter-egg rack: holds up to ten TACZ guns that orbit the wearer and fire
 * whenever the wearer does. The guns live in the stack's NBT so the rack can be
 * moved between the back slot and the inventory without losing them.
 */
public class SatelliteRackItem extends Item {
    public static final int SLOTS = 10;
    private static final String TAG_GUNS = "Guns";

    public SatelliteRackItem(Properties properties) {
        super(properties);
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
        tooltip.add(Component.translatable("item.taczammobox.satellite_rack.desc")
                .withStyle(ChatFormatting.GRAY));
        int loaded = loadedCount(stack);
        if (loaded > 0) {
            tooltip.add(Component.translatable("item.taczammobox.satellite_rack.loaded", loaded, SLOTS)
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("item.taczammobox.satellite_rack.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("item.taczammobox.satellite_rack.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** Set by the creative tab / tooltip code so the easter-egg tag stays in one place. */
    public static boolean isEasterEgg() {
        return FeatureFlags.easterEggTab();
    }
}
