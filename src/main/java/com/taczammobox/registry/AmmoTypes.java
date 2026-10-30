package com.taczammobox.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.taczammobox.AmmoType;
import com.taczammobox.TaczAmmoBoxMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The single source of truth for packaged ammo types.
 *
 * <p>Built-in types cover the TACZ default gun pack. Extra types (other gun packs)
 * come from {@code config/taczammobox/ammo_boxes.json}. The file is read during mod
 * construction, before item registration, because item ids have to exist up front.</p>
 */
public final class AmmoTypes {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** TACZ 1.1.8 default pack ammo: id suffix and a deterministic display colour. */
    private static final String[][] BUILTINS = {
            {"12g", "#B06B4F"},
            {"22wmr", "#CBB07A"},
            {"30_06", "#AE8E57"},
            {"308", "#A88F5C"},
            {"338", "#8B8F9A"},
            {"357mag", "#BFA06A"},
            {"40mm", "#7E8A5A"},
            {"45_70", "#B58A52"},
            {"45acp", "#C0A46B"},
            {"46x30", "#8FA36A"},
            {"500mag", "#D0A96A"},
            {"50ae", "#C9A76A"},
            {"50bmg", "#83879A"},
            {"545x39", "#94A566"},
            {"556x45", "#9CA86B"},
            {"57x28", "#9FA36A"},
            {"58x42", "#9AA76A"},
            {"68x51fury", "#9E8F5E"},
            {"762x25", "#B59A63"},
            {"762x39", "#A89A62"},
            {"762x54", "#B0925E"},
            {"792x57", "#B08A55"},
            {"9mm", "#B8A36A"},
            {"rpg_rocket", "#6F7A52"}
    };

    private static final List<AmmoType> ALL = new ArrayList<>();
    private static final Map<ResourceLocation, AmmoType> BY_ID = new LinkedHashMap<>();
    private static final Set<ResourceLocation> BUILTIN_IDS = new HashSet<>();

    private AmmoTypes() {
    }

    public static void init() {
        ALL.clear();
        BY_ID.clear();
        BUILTIN_IDS.clear();

        Path dir = configDir();
        Path file = dir.resolve("ammo_boxes.json");
        JsonObject root = readOrCreate(dir, file);

        Map<ResourceLocation, AmmoType> merged = new LinkedHashMap<>();
        boolean builtin = !root.has("builtin") || root.get("builtin").getAsBoolean();
        if (builtin) {
            for (String[] entry : BUILTINS) {
                ResourceLocation id = new ResourceLocation("tacz", entry[0]);
                BUILTIN_IDS.add(id);
                merged.put(id, new AmmoType(id, entry[0], "tacz.ammo." + entry[0] + ".name", entry[0], parseColor(entry[1])));
            }
        }

        if (root.has("entries") && root.get("entries").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("entries")) {
                mergeEntry(merged, element);
            }
        }

        List<AmmoType> sorted = new ArrayList<>(merged.values());
        sorted.sort(Comparator.comparing(type -> type.ammoId().toString()));

        Set<String> usedNames = new HashSet<>();
        for (AmmoType type : sorted) {
            String unique = type.itemName();
            int suffix = 2;
            while (!usedNames.add(unique.toLowerCase(Locale.ROOT))) {
                unique = type.itemName() + "_" + suffix++;
            }
            AmmoType resolved = unique.equals(type.itemName())
                    ? type
                    : new AmmoType(type.ammoId(), unique, type.nameKey(), type.fallbackName(), type.color());
            ALL.add(resolved);
            BY_ID.put(resolved.ammoId(), resolved);
        }

        TaczAmmoBoxMod.LOGGER.info("Registered {} packaged ammo types (builtin={})", ALL.size(), builtin);
    }

    private static void mergeEntry(Map<ResourceLocation, AmmoType> merged, JsonElement element) {
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (!object.has("ammo")) {
            TaczAmmoBoxMod.LOGGER.warn("Skipping ammo_boxes entry without 'ammo': {}", object);
            return;
        }
        ResourceLocation ammoId = ResourceLocation.tryParse(object.get("ammo").getAsString());
        if (ammoId == null) {
            TaczAmmoBoxMod.LOGGER.warn("Skipping ammo_boxes entry with invalid id: {}", object.get("ammo"));
            return;
        }

        AmmoType base = merged.get(ammoId);
        String itemName = object.has("id")
                ? sanitizeItemName(object.get("id").getAsString())
                : (base != null ? base.itemName() : sanitizeItemName(ammoId.getPath()));
        String nameKey = object.has("nameKey")
                ? object.get("nameKey").getAsString()
                : (base != null ? base.nameKey() : "");
        String fallback = object.has("name")
                ? object.get("name").getAsString()
                : (base != null ? base.fallbackName() : ammoId.getPath());
        int color = object.has("color")
                ? parseColor(object.get("color"))
                : (base != null ? base.color() : hashColor(ammoId.toString()));

        merged.put(ammoId, new AmmoType(ammoId, itemName, nameKey, fallback, color));
    }

    private static JsonObject readOrCreate(Path dir, Path file) {
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(dir);
                JsonObject root = new JsonObject();
                root.addProperty("_comment_cn",
                        "builtin=true 注册 TACZ 默认 24 种; entries 里追加第三方弹种 {ammo,id,nameKey,name,color}. 客户端与服务端必须使用同一份文件.");
                root.addProperty("_comment_en",
                        "builtin=true registers the 24 TACZ defaults; add third-party ammo under entries as {ammo,id,nameKey,name,color}. The client and server must use the same file.");
                root.addProperty("builtin", true);
                root.add("entries", new JsonArray());
                Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
                TaczAmmoBoxMod.LOGGER.info("Created default ammo config at {}", file);
                return root;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(text);
            if (parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
            TaczAmmoBoxMod.LOGGER.error("ammo_boxes.json root is not an object, using built-ins only");
        } catch (IOException | RuntimeException exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to read ammo_boxes.json, using built-ins only", exception);
        }
        JsonObject fallback = new JsonObject();
        fallback.addProperty("builtin", true);
        fallback.add("entries", new JsonArray());
        return fallback;
    }

    public static List<AmmoType> all() {
        return List.copyOf(ALL);
    }

    public static AmmoType byId(ResourceLocation ammoId) {
        return BY_ID.get(ammoId);
    }

    /** Built-in types are the only ones that ship block models and can have a crate block. */
    public static boolean isBuiltin(ResourceLocation ammoId) {
        return BUILTIN_IDS.contains(ammoId);
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get().resolve("taczammobox");
    }

    public static String sanitizeItemName(String raw) {
        String cleaned = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (cleaned.isEmpty()) {
            cleaned = "ammo";
        }
        return cleaned;
    }

    public static int parseColor(JsonElement element) {
        if (element == null) {
            return 0xC8B27A;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsInt() & 0xFFFFFF;
        }
        return parseColor(element.getAsString());
    }

    public static int parseColor(String raw) {
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.startsWith("0x") || value.startsWith("0X")) {
            value = value.substring(2);
        }
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException exception) {
            return 0xC8B27A;
        }
    }

    public static int hashColor(String key) {
        int hash = key.hashCode();
        float hue = ((hash & 0xFFFF) / 65535.0F);
        return hsbToRgb(hue, 0.38F, 0.72F);
    }

    private static int hsbToRgb(float hue, float saturation, float brightness) {
        float r = 0.0F;
        float g = 0.0F;
        float b = 0.0F;
        if (saturation == 0.0F) {
            r = g = b = brightness;
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0F;
            int sector = (int) Math.floor(h);
            float f = h - sector;
            float p = brightness * (1.0F - saturation);
            float q = brightness * (1.0F - saturation * f);
            float t = brightness * (1.0F - saturation * (1.0F - f));
            switch (sector) {
                case 0 -> {
                    r = brightness;
                    g = t;
                    b = p;
                }
                case 1 -> {
                    r = q;
                    g = brightness;
                    b = p;
                }
                case 2 -> {
                    r = p;
                    g = brightness;
                    b = t;
                }
                case 3 -> {
                    r = p;
                    g = q;
                    b = brightness;
                }
                case 4 -> {
                    r = t;
                    g = p;
                    b = brightness;
                }
                default -> {
                    r = brightness;
                    g = p;
                    b = q;
                }
            }
        }
        int red = Math.round(r * 255.0F);
        int green = Math.round(g * 255.0F);
        int blue = Math.round(b * 255.0F);
        return (red << 16) | (green << 8) | blue;
    }
}
