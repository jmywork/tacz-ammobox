package com.taczammobox.crafting;

import com.google.gson.JsonObject;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import org.jetbrains.annotations.Nullable;

/**
 * Matches a TACZ attachment by its attachment id instead of by a dedicated item.
 *
 * <p>TACZ stores every attachment in the single {@code tacz:attachment} item and puts the
 * id in NBT, so a normal ingredient cannot tell a sniper extended magazine from a scope.
 * This ingredient compares the NBT id, which is what the gun smith table recipes need.</p>
 */
public final class TaczAttachmentIngredient extends AbstractIngredient {
    private final ResourceLocation attachmentId;
    private final ItemStack preview;

    public TaczAttachmentIngredient(ResourceLocation attachmentId) {
        this.attachmentId = attachmentId;
        this.preview = AttachmentItemBuilder.create().setId(attachmentId).build();
    }

    public ResourceLocation attachmentId() {
        return attachmentId;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        IAttachment attachment = IAttachment.getIAttachmentOrNull(stack);
        return attachment != null && attachmentId.equals(attachment.getAttachmentId(stack));
    }

    @Override
    public ItemStack[] getItems() {
        return new ItemStack[]{preview.copy()};
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return AttachmentIngredientSerializer.INSTANCE;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", AttachmentIngredientSerializer.ID.toString());
        json.addProperty("id", attachmentId.toString());
        return json;
    }
}
