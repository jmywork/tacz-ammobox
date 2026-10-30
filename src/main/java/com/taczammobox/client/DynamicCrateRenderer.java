package com.taczammobox.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import com.taczammobox.AmmoType;
import com.taczammobox.block.DynamicCrateBlock;
import com.taczammobox.block.DynamicCrateBlockEntity;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.registry.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.taczammobox.TaczAmmoBoxMod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DynamicCrateRenderer implements BlockEntityRenderer<DynamicCrateBlockEntity> {
    private static final Map<ResourceLocation, Boolean> TEXTURE_CACHE = new HashMap<>();

    public DynamicCrateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DYNAMIC_CRATE.get(), DynamicCrateRenderer::new);
    }

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profiler) {
                TEXTURE_CACHE.clear();
            }
        });
    }

    private static boolean hasTexture(ResourceLocation texture) {
        return TEXTURE_CACHE.computeIfAbsent(texture,
                id -> Minecraft.getInstance().getResourceManager().getResource(id).isPresent());
    }

    @Override
    public void render(DynamicCrateBlockEntity crate, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ResourceLocation ammo = crate.getAmmoId();
        if (ammo == null) {
            return;
        }
        AmmoType type = AmmoTypes.byId(ammo);
        if (type == null) {
            return;
        }
        ResourceLocation texture = new ResourceLocation(TaczAmmoBoxMod.MOD_ID,
                "textures/item/top/crate_" + type.itemName() + ".png");
        if (!hasTexture(texture)) {
            return;
        }

        pose.pushPose();
        pose.translate(0.5F, 1.002F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-modelRotation(crate)));
        pose.translate(-0.5F, 0.0F, -0.5F);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        consumer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(255, 255, 255, 255)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, 1.0F, 0.0F, 0.0F).color(255, 255, 255, 255)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, 1.0F, 0.0F, 1.0F).color(255, 255, 255, 255)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, 0.0F, 0.0F, 1.0F).color(255, 255, 255, 255)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        pose.popPose();

        renderFrontLabel(crate, pose, buffer, packedLight);
    }

    private void renderFrontLabel(DynamicCrateBlockEntity crate, PoseStack pose,
                                  MultiBufferSource buffer, int packedLight) {
        AmmoType type = AmmoTypes.byId(crate.getAmmoId());
        if (type == null) {
            return;
        }
        ResourceLocation texture = new ResourceLocation(TaczAmmoBoxMod.MOD_ID,
                "textures/item/label/crate_" + type.itemName() + ".png");
        if (!hasTexture(texture)) {
            return;
        }

        float rotation = modelRotation(crate);
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-rotation));
        pose.translate(-0.5F, 0.0F, -0.5F);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        consumer.vertex(matrix, 0.0F, 0.0F, -0.01F).color(255, 255, 255, 255)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, 1.0F, 0.0F, -0.01F).color(255, 255, 255, 255)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, 1.0F, 1.0F, -0.01F).color(255, 255, 255, 255)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, 0.0F, 1.0F, -0.01F).color(255, 255, 255, 255)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 0.0F, -1.0F).endVertex();
        pose.popPose();
    }

    private static float modelRotation(DynamicCrateBlockEntity crate) {
        Direction facing = crate.getBlockState().getValue(DynamicCrateBlock.FACING);
        return (facing.toYRot() + 180.0F) % 360.0F;
    }
}
