package com.taczammobox.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.taczammobox.block.AmmoBoxPileBlock;
import com.taczammobox.block.AmmoBoxPileBlockEntity;
import com.taczammobox.registry.ModBlockEntities;
import com.taczammobox.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.taczammobox.TaczAmmoBoxMod;

/**
 * Draws the boxes on a pile. Six boxes fill one 2 x 3 layer; a second layer is
 * used for 7-12 boxes. Partial counts use centered arrangements instead of
 * filling from one corner.
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AmmoBoxPileRenderer implements BlockEntityRenderer<AmmoBoxPileBlockEntity> {
    private static final float[][] FULL = {
            {4.5F, 2.67F, 0.0F}, {11.5F, 2.67F, 0.0F},
            {4.5F, 8.0F, 0.0F}, {11.5F, 8.0F, 0.0F},
            {4.5F, 13.33F, 0.0F}, {11.5F, 13.33F, 0.0F}
    };
    private static final float[][] ONE = {{8.0F, 8.0F, 0.0F}};
    private static final float[][] TWO = {{4.5F, 8.0F, 0.0F}, {11.5F, 8.0F, 0.0F}};
    private static final float[][] THREE = {
            {4.5F, 8.0F, 0.0F}, {11.5F, 8.0F, 0.0F}, {8.0F, 8.0F, 1.0F}
    };
    private static final float[][] FOUR = {
            {4.5F, 5.33F, 0.0F}, {11.5F, 5.33F, 0.0F},
            {4.5F, 10.67F, 0.0F}, {11.5F, 10.67F, 0.0F}
    };
    private static final float[][] FIVE = {
            {4.5F, 5.33F, 0.0F}, {11.5F, 5.33F, 0.0F},
            {4.5F, 10.67F, 0.0F}, {11.5F, 10.67F, 0.0F}, {8.0F, 8.0F, 1.0F}
    };
    private static final float[][] ONE_TOP = {{8.0F, 8.0F, 1.0F}};
    private static final float[][] TWO_TOP = {
            {4.5F, 8.0F, 1.0F}, {11.5F, 8.0F, 1.0F}
    };
    private static final float[][] NINE = {
            {4.5F, 2.67F, 0.0F}, {11.5F, 2.67F, 0.0F},
            {4.5F, 8.0F, 0.0F}, {11.5F, 8.0F, 0.0F},
            {4.5F, 13.33F, 0.0F}, {11.5F, 13.33F, 0.0F},
            {8.0F, 2.67F, 1.0F}, {8.0F, 8.0F, 1.0F}, {8.0F, 13.33F, 1.0F}
    };
    private static final float[][] TEN = {
            {4.5F, 2.67F, 0.0F}, {11.5F, 2.67F, 0.0F},
            {4.5F, 8.0F, 0.0F}, {11.5F, 8.0F, 0.0F},
            {4.5F, 13.33F, 0.0F}, {11.5F, 13.33F, 0.0F},
            {4.5F, 5.33F, 1.0F}, {11.5F, 5.33F, 1.0F},
            {4.5F, 10.67F, 1.0F}, {11.5F, 10.67F, 1.0F}
    };
    private static final float[][] ELEVEN = {
            {4.5F, 2.67F, 0.0F}, {11.5F, 2.67F, 0.0F},
            {4.5F, 8.0F, 0.0F}, {11.5F, 8.0F, 0.0F},
            {4.5F, 13.33F, 0.0F}, {11.5F, 13.33F, 0.0F},
            {4.5F, 2.67F, 1.0F}, {11.5F, 2.67F, 1.0F},
            {4.5F, 8.0F, 1.0F}, {11.5F, 8.0F, 1.0F},
            {4.5F, 13.33F, 1.0F}
    };
    private static final float[][] TWELVE = {
            {4.5F, 2.67F, 0.0F}, {11.5F, 2.67F, 0.0F},
            {4.5F, 8.0F, 0.0F}, {11.5F, 8.0F, 0.0F},
            {4.5F, 13.33F, 0.0F}, {11.5F, 13.33F, 0.0F},
            {4.5F, 2.67F, 1.0F}, {11.5F, 2.67F, 1.0F},
            {4.5F, 8.0F, 1.0F}, {11.5F, 8.0F, 1.0F},
            {4.5F, 13.33F, 1.0F}, {11.5F, 13.33F, 1.0F}
    };
    private static final float SHIFT_Y = 4.0F;

    public AmmoBoxPileRenderer(BlockEntityRendererProvider.Context context) {
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BOX_PILE.get(), AmmoBoxPileRenderer::new);
    }

    @Override
    public void render(AmmoBoxPileBlockEntity pile, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ResourceLocation ammo = pile.getAmmoId();
        if (ammo == null) {
            return;
        }
        Item item = ModItems.box(ammo);
        if (item == null) {
            return;
        }
        BlockState state = pile.getBlockState();
        int count = state.hasProperty(AmmoBoxPileBlock.BOXES)
                ? state.getValue(AmmoBoxPileBlock.BOXES) : 1;
        count = Math.max(1, Math.min(AmmoBoxPileBlock.MAX_BOXES, count));
        float[][] layout = layout(count);

        // The layout is authored facing north; rotate the grid around the block
        // centre so a placed pile follows the direction of whoever placed it.
        Direction facing = state.hasProperty(AmmoBoxPileBlock.FACING)
                ? state.getValue(AmmoBoxPileBlock.FACING) : Direction.NORTH;
        float radians = (float) Math.toRadians(switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        });
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        ItemStack stack = new ItemStack(item);
        int seed = (int) pile.getBlockPos().asLong();
        for (int i = 0; i < layout.length; i++) {
            float[] slot = layout[i];
            float dx = slot[0] - 8.0F;
            float dz = slot[1] - 8.0F;
            float rx = dx * cos - dz * sin;
            float rz = dx * sin + dz * cos;
            pose.pushPose();
            // ItemRenderer itself applies a -0.5 block translation after the
            // model transform, so compensate here and keep every box centered
            // on its grid cell.
            pose.translate((8.0F + rx + 0.5F) / 16.0F,
                    slot[2] * 6.0F / 16.0F + SHIFT_Y / 16.0F,
                    (8.0F + rz + 0.25F) / 16.0F);
            pose.scale(0.4666667F, 0.5F, 0.5F);
            renderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight, packedOverlay,
                    pose, buffer, pile.getLevel(), seed + i);
            pose.popPose();
        }
    }

    private static float[][] layout(int count) {
        if (count <= 1) {
            return ONE;
        }
        if (count == 2) {
            return TWO;
        }
        if (count == 3) {
            return THREE;
        }
        if (count == 4) {
            return FOUR;
        }
        if (count == 5) {
            return FIVE;
        }
        if (count == 6) {
            return FULL;
        }
        if (count == 7) {
            return concat(FULL, ONE_TOP);
        }
        if (count == 8) {
            return concat(FULL, TWO_TOP);
        }
        if (count == 9) {
            return NINE;
        }
        if (count == 10) {
            return TEN;
        }
        if (count == 11) {
            return ELEVEN;
        }
        return TWELVE;
    }

    private static float[][] concat(float[][] first, float[][] second) {
        float[][] result = new float[first.length + second.length][];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}

