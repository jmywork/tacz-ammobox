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
 * Draws the boxes on a pile. Six boxes are two columns by three layers; the
 * lower layers are filled first so a partly filled pile still looks natural.
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AmmoBoxPileRenderer implements BlockEntityRenderer<AmmoBoxPileBlockEntity> {
    private static final int[][][] LAYOUTS = {
            {},
            {{8, 0}},
            {{4, 0}, {12, 0}},
            {{4, 0}, {12, 0}, {8, 1}},
            {{4, 0}, {12, 0}, {4, 1}, {12, 1}},
            {{4, 0}, {12, 0}, {4, 1}, {12, 1}, {8, 2}},
            {{4, 0}, {12, 0}, {4, 1}, {12, 1}, {4, 2}, {12, 2}}
    };

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

        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        ItemStack stack = new ItemStack(item);
        int seed = (int) pile.getBlockPos().asLong();
        for (int i = 0; i < LAYOUTS[count].length; i++) {
            int[] pos = LAYOUTS[count][i];
            pose.pushPose();
            // The box item model is 15 x 6 x 8 pixels. At 0.5 scale it is
            // 7.5 wide, so two columns fit with a small gap.
            pose.translate(pos[0] / 16.0F - 0.25F, pos[1] * 3.0F / 16.0F, 0.25F);
            pose.scale(0.5F, 0.5F, 0.5F);
            renderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight, packedOverlay,
                    pose, buffer, pile.getLevel(), seed + i);
            pose.popPose();
        }
    }
}
