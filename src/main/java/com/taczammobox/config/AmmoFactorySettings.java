package com.taczammobox.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.taczammobox.TaczAmmoBoxMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Tuning for the ammo assembler.
 *
 * <p>{@code ammo_factory.json} holds a global default plus optional per-ammo overrides:
 * how many rounds one craft yields and how long it takes. Unknown ammo ids simply use the
 * defaults, so third-party gun packs work without extra configuration.</p>
 */
public final class AmmoFactorySettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, Integer> ROUNDS = new HashMap<>();
    private static final Map<ResourceLocation, Integer> TICKS = new HashMap<>();

    private static int defaultRounds = 4;
    private static int defaultTicks = 60;

    private AmmoFactorySettings() {
    }

    public static int roundsFor(ResourceLocation ammoId) {
        Integer value = ROUNDS.get(ammoId);
        return value == null ? defaultRounds : Math.max(1, value);
    }

    public static int ticksFor(ResourceLocation ammoId) {
        Integer value = TICKS.get(ammoId);
        return value == null ? defaultTicks : Math.max(1, value);
    }

    public static void load() {
        ROUNDS.clear();
        TICKS.clear();
        Path file = file();
        try {
            if (Files.exists(file)) {
                JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                defaultRounds = clamp(root, "defaultRounds", defaultRounds, 1, 64);
                defaultTicks = clamp(root, "defaultTicks", defaultTicks, 1, 1200);
                readMap(root, "rounds", ROUNDS, 1, 64);
                readMap(root, "ticks", TICKS, 1, 1200);
            }
        } catch (Exception exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to read ammo_factory.json, using defaults", exception);
        }
        save();
    }

    private static int clamp(JsonObject root, String key, int fallback, int min, int max) {
        if (!root.has(key)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, root.get(key).getAsInt()));
    }

    private static void readMap(JsonObject root, String key, Map<ResourceLocation, Integer> target,
                                int min, int max) {
        if (!root.has(key) || !root.get(key).isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject(key).entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id != null) {
                target.put(id, Math.max(min, Math.min(max, entry.getValue().getAsInt())));
            }
        }
    }

    private static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("_comment_cn",
                    "弹药装配机参数。defaultRounds 每次合成产出多少发，defaultTicks 每次合成耗时(tick)。"
                            + "rounds / ticks 可按弹种覆盖，键是 TACZ 弹药 id（例如 tacz:556x45）。");
            root.addProperty("_comment_en",
                    "Ammo assembler tuning. defaultRounds = rounds per craft, defaultTicks = craft time. "
                            + "Use rounds / ticks maps keyed by TACZ ammo id (e.g. tacz:556x45) to override.");
            root.addProperty("defaultRounds", defaultRounds);
            root.addProperty("defaultTicks", defaultTicks);

            JsonObject rounds = new JsonObject();
            ROUNDS.forEach((id, value) -> rounds.addProperty(id.toString(), value));
            JsonObject ticks = new JsonObject();
            TICKS.forEach((id, value) -> ticks.addProperty(id.toString(), value));
            root.add("rounds", rounds);
            root.add("ticks", ticks);
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to save ammo_factory.json", exception);
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("taczammobox").resolve("ammo_factory.json");
    }
}
