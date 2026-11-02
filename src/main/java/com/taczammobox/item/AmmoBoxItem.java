package com.taczammobox.item;

import com.taczammobox.AmmoType;
import com.taczammobox.block.AmmoBoxPileBlock;
import com.taczammobox.block.AmmoBoxPileBlockEntity;
import com.taczammobox.registry.ModBlocks;
import com.taczammobox.util.AmmoBoxHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

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
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level level,
                                java.util.List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("item.taczammobox.box.place")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
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
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);
        ItemStack held = context.getItemInHand();

        // Right-click an existing pile to add one matching box.
        if (clicked.getBlock() instanceof AmmoBoxPileBlock) {
            if (!level.isClientSide) {
                boolean added = tryAddToPile(level, pos, clicked, player, held);
                if (!added && player.isShiftKeyDown()) {
                    tryPlacePile(level, pos.above(), player, held);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Right-click any block to place a new pile. Right-clicking air still
        // unpacks into loose ammo, so block interactions never consume the box
        // as a loose-ammo conversion by accident.
        if (!level.isClientSide) {
            tryPlacePile(level, pos.relative(context.getClickedFace()), player, held);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean tryAddToPile(Level level, BlockPos pos, BlockState state, Player player, ItemStack held) {
        if (!(level.getBlockEntity(pos) instanceof AmmoBoxPileBlockEntity pile)
                || !type.ammoId().equals(pile.getAmmoId())) {
            return false;
        }
        int count = state.getValue(AmmoBoxPileBlock.BOXES);
        if (count >= AmmoBoxPileBlock.MAX_BOXES) {
            return false;
        }
        level.setBlock(pos, state.setValue(AmmoBoxPileBlock.BOXES, count + 1), 3);
        consume(player, held);
        level.playSound(null, pos, SoundType.WOOD.getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
        return true;
    }

    private boolean tryPlacePile(Level level, BlockPos pos, Player player, ItemStack held) {
        if (!level.getBlockState(pos).canBeReplaced()
                || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, net.minecraft.core.Direction.UP, held)) {
            return false;
        }
        level.setBlock(pos, ModBlocks.BOX_PILE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof AmmoBoxPileBlockEntity pile) {
            pile.setAmmoId(type.ammoId());
        }
        consume(player, held);
        level.playSound(null, pos, SoundType.WOOD.getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
        return true;
    }

    private static void consume(Player player, ItemStack held) {
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
    }
}
