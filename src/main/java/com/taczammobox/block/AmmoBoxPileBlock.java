package com.taczammobox.block;

import com.taczammobox.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A small stack of packaged ammo boxes inside one block space. Six boxes are
 * arranged in two columns and three layers; the pallet base is part of the
 * static model and the boxes themselves are drawn by a block entity renderer
 * so each ammo type keeps its own label.
 */
public class AmmoBoxPileBlock extends BaseEntityBlock {
    public static final int MAX_BOXES = 6;
    public static final IntegerProperty BOXES = IntegerProperty.create("boxes", 1, MAX_BOXES);

    private static final VoxelShape[] SHAPES = new VoxelShape[MAX_BOXES + 1];

    static {
        for (int i = 1; i <= MAX_BOXES; i++) {
            SHAPES[i] = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D + i * 1.5D, 16.0D);
        }
    }

    public AmmoBoxPileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BOXES, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BOXES);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(BOXES)];
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AmmoBoxPileBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        // Sneak + empty hand removes one box from the top of the pile.
        if (level.isClientSide || !player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        int count = state.getValue(BOXES);
        if (count <= 1) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof AmmoBoxPileBlockEntity pile)
                || pile.getAmmoId() == null) {
            return InteractionResult.PASS;
        }
        ItemStack drop = new ItemStack(ModItems.box(pile.getAmmoId()));
        if (drop.isEmpty()) {
            return InteractionResult.PASS;
        }
        level.setBlock(pos, state.setValue(BOXES, count - 1), 3);
        if (!player.getAbilities().instabuild) {
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                SoundSource.BLOCKS, 0.6F, 1.0F);
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        dropPile(level, pos, state, blockEntity);
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        dropPile(level, pos, state, level.getBlockEntity(pos));
        super.onBlockExploded(state, level, pos, explosion);
    }

    private static void dropPile(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (level.isClientSide || !(blockEntity instanceof AmmoBoxPileBlockEntity pile)
                || pile.getAmmoId() == null) {
            return;
        }
        ItemStack stack = new ItemStack(ModItems.box(pile.getAmmoId()), state.getValue(BOXES));
        if (!stack.isEmpty()) {
            Block.popResource(level, pos, stack);
        }
    }
}
