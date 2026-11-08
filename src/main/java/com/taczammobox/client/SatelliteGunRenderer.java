package com.taczammobox.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.taczammobox.entity.SatelliteGunEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws the held gun with TACZ's own item renderer, then tilts it toward the
 * focus point the manager computed. No custom model, so every gun pack works.
 */
public class SatelliteGunRenderer extends EntityRenderer<SatelliteGunEntity> {
    private static final ResourceLocation FALLBACK = new ResourceLocation("minecraft", "textures/misc/white.png");
    /** The flash texture TACZ ships with its default gun pack. */
    private static final ResourceLocation FLASH = new ResourceLocation("tacz", "textures/flash/common_muzzle_flash.png");

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

        renderFlash(entity, partialTick, pose, buffers);
    }

    /**
     * Draws TACZ's own muzzle flash texture as a camera-facing quad while the
     * entity's flash timer is running.
     */
    private void renderFlash(SatelliteGunEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        int flash = entity.getFlash();
        if (flash <= 0) {
            return;
        }
        if (Minecraft.getInstance().getResourceManager().getResource(FLASH).isEmpty()) {
            return;
        }
        Vec3 look = entity.getViewVector(partialTick);
        pose.pushPose();
        pose.translate(look.x * 0.55D, look.y * 0.55D, look.z * 0.55D);
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        float size = 0.45F + 0.18F * flash;
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(FLASH));
        int alpha = Math.min(255, 90 + flash * 80);
        consumer.vertex(matrix, -size, -size, 0.0F).color(255, 255, 255, alpha).uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, size, -size, 0.0F).color(255, 255, 255, alpha).uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, size, size, 0.0F).color(255, 255, 255, alpha).uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, -size, size, 0.0F).color(255, 255, 255, alpha).uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0.0F, 0.0F, 1.0F).endVertex();
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SatelliteGunEntity entity) {
        return FALLBACK;
    }
}
