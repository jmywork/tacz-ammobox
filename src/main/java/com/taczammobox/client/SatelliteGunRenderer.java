package com.taczammobox.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager;
import com.taczammobox.entity.SatelliteGunEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the held gun with TACZ's own item renderer, then tilts it toward the
 * focus point the manager computed. No custom model, so every gun pack works.
 *
 * <p>The muzzle flash is not ours: TACZ looks the shooting entity up through
 * {@link ThirdPersonMuzzleParticleManager#currentEntity}, so we point that
 * static at the player for the duration of the draw. Its own particle emitter
 * and flash texture then spawn at this satellite's muzzle.</p>
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
        Player player = Minecraft.getInstance().player;
        if (player != null && entity.consumeFlashStart()) {
            ThirdPersonMuzzleParticleManager.onShoot(player, stack);
            MuzzleFlashRender.onEntityShoot(player);
        }

        pose.pushPose();

        // TACZ renders guns with the stock on +X and the barrel on -X under FIXED.
        // Turning +X onto +Z first lets pitch tilt the barrel properly; the
        // resulting -X then ends up pointing along the look vector.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        // Roll around the barrel: the fan lays every gun broadside to the viewer,
        // so the arc of rifles reads as one opened fan instead of a row of sticks.
        pose.mulPose(Axis.XP.rotationDegrees(entity.getRoll()));

        pose.scale(0.5F, 0.5F, 0.5F);
        // TACZ's FIXED draw offsets the model to the block centre (0.5, 2, 0.5)
        // before its own positioning nodes. That offset lies along the gun's thin
        // axis, so on a radial fan it pushes one half of the fan backwards and the
        // other forwards. Cancel the depth part to put both halves on one plane.
        pose.translate(0.0F, 0.0F, -0.5F);

        LivingEntity previous = ThirdPersonMuzzleParticleManager.currentEntity;
        ThirdPersonMuzzleParticleManager.currentEntity = player;
        try {
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
                    OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        } finally {
            ThirdPersonMuzzleParticleManager.currentEntity = previous;
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SatelliteGunEntity entity) {
        return FALLBACK;
    }
}
