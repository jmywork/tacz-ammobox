package com.taczammobox.item;

import com.taczammobox.AmmoType;
import com.taczammobox.util.AmmoBoxHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A belt that feeds the main-hand gun from boxes/crates in the player's
 * inventory. The belt stores a partial magazine buffer in its own NBT so a
 * 60-round box can feed a 30-round magazine without wasting the rest.
 */
public class AmmoBeltItem extends Item {
    private static final String TAG_AMMO = "AmmoId";
    private static final String TAG_ROUNDS = "Rounds";

    public AmmoBeltItem(Properties properties) {
        super(properties);
    }

    public static ResourceLocation storedAmmoId(ItemStack belt) {
        CompoundTag tag = belt.getTag();
        if (tag == null || !tag.contains(TAG_AMMO)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(TAG_AMMO));
    }

    public static int storedRounds(ItemStack belt) {
        CompoundTag tag = belt.getTag();
        return tag == null ? 0 : Math.max(0, tag.getInt(TAG_ROUNDS));
    }

    public static void store(ItemStack belt, ResourceLocation ammoId, int rounds) {
        CompoundTag tag = belt.getOrCreateTag();
        tag.putString(TAG_AMMO, ammoId.toString());
        tag.putInt(TAG_ROUNDS, Math.max(0, rounds));
    }

    public static void clearStore(ItemStack belt) {
        CompoundTag tag = belt.getTag();
        if (tag != null) {
            tag.remove(TAG_AMMO);
            tag.remove(TAG_ROUNDS);
        }
    }

    /** Rounds represented by one item: a box is one stack, a crate is nine. */
    public static int roundsPerItem(AmmoType type, int boxesPerItem) {
        return AmmoBoxHelper.roundsPerBox(type) * Math.max(1, boxesPerItem);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.taczammobox.ammo_belt.desc")
                .withStyle(ChatFormatting.GRAY));
        ResourceLocation stored = storedAmmoId(stack);
        int rounds = storedRounds(stack);
        if (stored != null && rounds > 0) {
            tooltip.add(Component.translatable("item.taczammobox.ammo_belt.buffer",
                    stored.toString(), rounds).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("item.taczammobox.ammo_belt.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("item.taczammobox.ammo_belt.quote")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
