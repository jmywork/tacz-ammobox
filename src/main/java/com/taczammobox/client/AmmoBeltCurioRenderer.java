package com.taczammobox.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.registry.ModItems;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Draws {@link AmmoBeltWornModel} on the player when the belt is equipped in a
 * Curios slot. Only loaded when Curios is present; see {@link ClientSetup}.
 */
public final class AmmoBeltCurioRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TaczAmmoBoxMod.MOD_ID, "textures/entity/ammo_belt_worn.png");

    private final AmmoBeltWornModel model = new AmmoBeltWornModel();
    /** Set when rendering fails once, so a bad frame cannot crash the client every tick. */
    private boolean disabled;

    public static void register() {
        CuriosRendererRegistry.register(ModItems.AMMO_BELT.get(), AmmoBeltCurioRenderer::new);
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
            PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource bufferSource,
            int light, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
            float netHeadYaw, float headPitch) {
        LivingEntity entity = slotContext.entity();
        if (entity == null || disabled) {
            return;
        }
        try {
            model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            ICurioRenderer.followBodyRotations(entity, model);
            ICurioRenderer.translateIfSneaking(poseStack, entity);
            // Passing a RenderType here makes Curios pair it with the model's own one;
            // when both are entityCutoutNoCull the game throws "Duplicate delegates".
            ICurioRenderer.renderModel(model, TEXTURE, poseStack, bufferSource, light, null);
        } catch (RuntimeException exception) {
            disabled = true;
            TaczAmmoBoxMod.LOGGER.error("Disabling ammo belt curio rendering after a render error", exception);
        }
    }
}
