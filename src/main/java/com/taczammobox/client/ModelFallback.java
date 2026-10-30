package com.taczammobox.client;

import com.taczammobox.AmmoType;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.registry.AmmoTypes;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Gives every registered ammo type a model even when no model file exists for it.
 *
 * <p>Built-in types ship real model files. Types added through
 * {@code config/taczammobox/ammo_boxes.json} have no art; here they inherit the
 * baked generic box/crate model. A resource pack can still override any item by
 * providing {@code models/item/box_<id>.json} — {@code putIfAbsent} keeps it.</p>
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModelFallback {
    private ModelFallback() {
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        BakedModel box = find(models, "box_generic");
        BakedModel crate = find(models, "crate_generic");

        for (AmmoType type : AmmoTypes.all()) {
            if (box != null) {
                register(models, "box_" + type.itemName(), box);
            }
            if (crate != null) {
                register(models, "crate_" + type.itemName(), crate);
            }
        }
    }

    private static void register(Map<ResourceLocation, BakedModel> models, String path, BakedModel model) {
        ResourceLocation id = new ResourceLocation(TaczAmmoBoxMod.MOD_ID, path);
        // 1.20.1 item models are normally baked under "<id>#inventory", but
        // missing-model lookups can use the plain id as well. Register both.
        putIfMissing(models, new ModelResourceLocation(id, "inventory"), model);
        putIfMissing(models, id, model);
    }

    private static void putIfMissing(Map<ResourceLocation, BakedModel> models, ResourceLocation key, BakedModel model) {
        BakedModel existing = models.get(key);
        // Forge inserts a MissingModel for unknown item ids; putIfAbsent would
        // keep that purple/black placeholder, so replace it explicitly.
        if (existing == null || existing.getClass().getSimpleName().toLowerCase().contains("missing")) {
            models.put(key, model);
        }
    }

    private static BakedModel find(Map<ResourceLocation, BakedModel> models, String path) {
        BakedModel model = models.get(itemLocation(path));
        if (model == null) {
            model = models.get(new ResourceLocation(TaczAmmoBoxMod.MOD_ID, path));
        }
        return model;
    }

    private static ModelResourceLocation itemLocation(String path) {
        return new ModelResourceLocation(new ResourceLocation(TaczAmmoBoxMod.MOD_ID, path), "inventory");
    }
}
