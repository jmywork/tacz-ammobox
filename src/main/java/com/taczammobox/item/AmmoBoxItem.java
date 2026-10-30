package com.taczammobox.item;

import com.taczammobox.AmmoType;
import com.taczammobox.util.AmmoBoxHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class AmmoBoxItem extends Item implements PackagedAmmo {
    private final AmmoType type;

    public AmmoBoxItem(AmmoType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public AmmoType getType() {
        return type;
    }

    @Override
    public int boxesPerItem() {
        return 1;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.taczammobox.box", type.displayName());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean ok = AmmoBoxHelper.unbox(level, player, stack, type, 1);
        return ok ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        InteractionResultHolder<ItemStack> result = use(context.getLevel(), player, context.getHand());
        return result.getResult().consumesAction()
                ? InteractionResult.sidedSuccess(context.getLevel().isClientSide)
                : InteractionResult.PASS;
    }
}
