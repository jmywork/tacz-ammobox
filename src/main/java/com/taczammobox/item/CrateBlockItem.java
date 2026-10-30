package com.taczammobox.item;

import com.taczammobox.AmmoType;
import com.taczammobox.util.AmmoBoxHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * BlockItem for a decorative crate. Right click (including while sneaking)
 * behaves exactly like a normal block item; filling an ammo box is done with
 * the dedicated keybind.
 */
public class CrateBlockItem extends BlockItem implements PackagedAmmo {
    private final AmmoType type;

    public CrateBlockItem(Block block, AmmoType type, Properties properties) {
        super(block, properties);
        this.type = type;
    }

    @Override
    public AmmoType getType() {
        return type;
    }

    @Override
    public int boxesPerItem() {
        return AmmoBoxHelper.BOXES_PER_CRATE;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.taczammobox.crate", type.displayName());
    }
}
