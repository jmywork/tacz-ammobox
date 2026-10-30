package com.taczammobox;

import com.tacz.guns.api.TimelessAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * One packaged ammo type: the TACZ ammo id, the item id suffix, a fallback
 * translation key, the literal fallback name and the box colour.
 */
public record AmmoType(ResourceLocation ammoId, String itemName, String nameKey, String fallbackName, int color) {

    /**
     * Resolve the display name through TACZ's own ammo index when it is loaded,
     * so gun packs that ship their own lang keys are localised for free.
     * If the key has no translation, prettify the literal path instead of
     * exposing a raw translation key to the player.
     */
    public Component displayName() {
        String key = TimelessAPI.getCommonAmmoIndex(ammoId)
                .map(index -> index.getPojo().getName())
                .filter(name -> name != null && !name.isBlank())
                .orElse(nameKey);
        if (key != null && !key.isBlank()) {
            Component translated = Component.translatable(key);
            if (!translated.getString().equals(key)) {
                return translated;
            }
        }
        return Component.literal(readableFallback());
    }

    private String readableFallback() {
        String name = fallbackName == null ? "" : fallbackName.trim();
        if (name.isEmpty()) {
            name = ammoId.getPath();
        }
        name = name.replace('_', ' ').replace('-', ' ').trim();
        return name.toUpperCase(java.util.Locale.ROOT);
    }
}
