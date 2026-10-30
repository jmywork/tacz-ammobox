package com.taczammobox.item;

import com.taczammobox.AmmoType;
import com.taczammobox.block.DynamicCrateBlockEntity;
import com.taczammobox.util.AmmoBoxHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public class DynamicCrateBlockItem extends BlockItem implements PackagedAmmo {
    private final AmmoType type;

    public DynamicCrateBlockItem(Block block, AmmoType type, Properties properties) {
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

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result.consumesAction()) {
            BlockPos pos = context.getClickedPos();
            if (!(context.getLevel().getBlockEntity(pos) instanceof DynamicCrateBlockEntity)) {
                pos = pos.relative(context.getClickedFace());
            }
            // Set on both sides: the server syncs it, while the client copy makes
            // the BER show the label immediately even if the update packet is late.
            if (context.getLevel().getBlockEntity(pos) instanceof DynamicCrateBlockEntity crate) {
                crate.setAmmoId(type.ammoId());
            }
        }
        return result;
    }
}
