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
    // 2 columns (x) x 3 rows (z), filled bottom layer first.
    private static final float[][] SLOTS = {
            {4.0F, 2.67F},
            {12.0F, 2.67F},
            {4.0F, 8.0F},
            {12.0F, 8.0F},
            {4.0F, 13.33F},
            {12.0F, 13.33F}
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
        for (int i = 0; i < count; i++) {
            float[] slot = SLOTS[i % 6];
            float layer = i / 6;
            pose.pushPose();
            // ItemRenderer itself applies a -0.5 block translation after the
            // model transform, so compensate here and keep every box centered
            // on its 2 x 3 grid cell.
            pose.translate((slot[0] - 3.75F) / 16.0F, layer * 3.0F / 16.0F,
                    (slot[1] - 3.75F) / 16.0F);
            pose.scale(0.5F, 0.5F, 0.5F);
            renderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight, packedOverlay,
                    pose, buffer, pile.getLevel(), seed + i);
            pose.popPose();
        }
    }
}
