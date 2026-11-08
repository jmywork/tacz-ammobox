package com.taczammobox.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.taczammobox.entity.SatelliteGunEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the held gun with TACZ's own item renderer, then tilts it toward the
 * focus point the manager computed. No custom model, so every gun pack works.
 */
public class SatelliteGunRenderer extends EntityRenderer<SatelliteGunEntity> {
    private static final ResourceLocation FALLBACK = new ResourceLocation("minecraft", "textures/misc/white.png");

    public SatelliteGunRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SatelliteGunEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        ItemStack stack = entity.getGun();
        if (stack.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
        pose.mulPose(Axis.ZP.rotationDegrees(entity.getRoll()));
        pose.scale(1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SatelliteGunEntity entity) {
        return FALLBACK;
    }
}
