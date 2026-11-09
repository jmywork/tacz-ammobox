package com.taczammobox.crafting;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.taczammobox.TaczAmmoBoxMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;

/** Serializer for {@link TaczAttachmentIngredient}; JSON form is {@code {"type":"taczammobox:attachment","id":"..."}}. */
public enum AttachmentIngredientSerializer implements IIngredientSerializer<TaczAttachmentIngredient> {
    INSTANCE;

    public static final ResourceLocation ID = new ResourceLocation(TaczAmmoBoxMod.MOD_ID, "attachment");

    @Override
    public TaczAttachmentIngredient parse(FriendlyByteBuf buffer) {
        return new TaczAttachmentIngredient(buffer.readResourceLocation());
    }

    @Override
    public TaczAttachmentIngredient parse(JsonObject json) {
        String rawId = GsonHelper.getAsString(json, "id", "");
        ResourceLocation attachmentId = ResourceLocation.tryParse(rawId);
        if (attachmentId == null) {
            throw new JsonSyntaxException("Invalid attachment id '" + rawId + "'");
        }
        return new TaczAttachmentIngredient(attachmentId);
    }

    @Override
    public void write(FriendlyByteBuf buffer, TaczAttachmentIngredient ingredient) {
        buffer.writeResourceLocation(ingredient.attachmentId());
    }
}
