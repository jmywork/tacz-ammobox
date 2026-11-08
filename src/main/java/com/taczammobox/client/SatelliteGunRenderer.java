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

        // Kick the gun back along the aim vector; the value decays on the client.
        double yawRad = Math.toRadians(entity.getYRot());
        double pitchRad = Math.toRadians(entity.getXRot());
        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);
        float kick = entity.getRecoil() * 0.22F;
        pose.translate(-lookX * kick, -lookY * kick, -lookZ * kick);

        // TACZ renders guns with the stock on +X and the barrel on -X under FIXED.
        // Turning +X onto +Z first lets pitch tilt the barrel properly; the
        // resulting -X then ends up pointing along the look vector.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));

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
