package com.taczammobox.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tacz.guns.api.TimelessAPI;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.registry.AmmoTypes;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Detects ammo types that are loaded by TACZ but missing from the config and
 * appends them. Item ids cannot be registered mid-game, so a restart is needed
 * before the new box/crate items appear.
 */
public final class AmmoScanner {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AmmoScanner() {
    }

    public static int appendMissing() {
        return scan(false);
    }

    public static int pruneMissing() {
        return scan(true);
    }

    private static int scan(boolean prune) {
        Path dir = AmmoTypes.configDir();
        Path file = dir.resolve("ammo_boxes.json");
        try {
            JsonObject root;
            if (Files.exists(file)) {
                root = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            } else {
                root = new JsonObject();
                root.addProperty("builtin", true);
                root.add("entries", new JsonArray());
            }
            if (root == null) {
                root = new JsonObject();
                root.addProperty("builtin", true);
                root.add("entries", new JsonArray());
            }

            JsonArray entries = root.has("entries") && root.get("entries").isJsonArray()
                    ? root.getAsJsonArray("entries")
                    : new JsonArray();
            root.add("entries", entries);

            Set<ResourceLocation> loaded = new HashSet<>();
            for (var entry : TimelessAPI.getAllCommonAmmoIndex()) {
                loaded.add(entry.getKey());
            }

            int removed = 0;
            if (prune) {
                for (int i = entries.size() - 1; i >= 0; i--) {
                    JsonElement element = entries.get(i);
                    if (!element.isJsonObject() || !element.getAsJsonObject().has("ammo")) {
                        continue;
                    }
                    ResourceLocation id = ResourceLocation.tryParse(
                            element.getAsJsonObject().get("ammo").getAsString());
                    if (id == null) {
                        continue;
                    }
                    if (!loaded.contains(id) && !AmmoTypes.isBuiltin(id)) {
                        entries.remove(i);
                        removed++;
                    }
                }
            }

            Set<ResourceLocation> known = new HashSet<>();
            if (!root.has("builtin") || root.get("builtin").getAsBoolean()) {
                for (var type : AmmoTypes.all()) {
                    if (AmmoTypes.isBuiltin(type.ammoId())) {
                        known.add(type.ammoId());
                    }
                }
            }
            for (JsonElement element : entries) {
                if (element.isJsonObject() && element.getAsJsonObject().has("ammo")) {
                    ResourceLocation id = ResourceLocation.tryParse(
                            element.getAsJsonObject().get("ammo").getAsString());
                    if (id != null) {
                        known.add(id);
                    }
                }
            }

            int added = 0;
            if (!prune) {
                for (ResourceLocation id : loaded) {
                    if (known.contains(id)) {
                        continue;
                    }
                    JsonObject object = new JsonObject();
                    object.addProperty("ammo", id.toString());
                    object.addProperty("id", AmmoTypes.sanitizeItemName(id.getPath()));
                    object.addProperty("color", String.format("#%06X", AmmoTypes.hashColor(id.toString())));
                    entries.add(object);
                    known.add(id);
                    added++;
                }
            }

            if (added > 0 || removed > 0) {
                Files.createDirectories(dir);
                Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
                TaczAmmoBoxMod.LOGGER.info("Updated ammo_boxes.json: added={}, removed={}; restart required",
                        added, removed);
            }
            return prune ? removed : added;
        } catch (Exception exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to scan ammo types", exception);
            return -1;
        }
    }
}
