package com.taczammobox.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.taczammobox.entity.SatelliteGunEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.math.Axis;
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

        // TACZ renders guns with the barrel on +X and the sights on +Y under
        // FIXED, so the muzzle has to be turned onto -Z before pitch is applied.
        // Order matters: yaw, then pitch, then the fixed 90 degree barrel turn.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));

        pose.scale(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SatelliteGunEntity entity) {
        return FALLBACK;
    }
}
