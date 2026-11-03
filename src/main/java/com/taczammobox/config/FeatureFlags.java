package com.taczammobox.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.taczammobox.TaczAmmoBoxMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Feature switches for the parts of the mod that hook into other mods.
 *
 * <p>Everything defaults to on, so an untouched install behaves exactly like 0.3.0.
 * Turning a switch off leaves the content in place but stops the corresponding
 * injection from doing anything, which is the escape hatch when a TACZ or Create
 * update breaks one specific behaviour.</p>
 */
public final class FeatureFlags {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean autoFire = true;
    private static boolean capacityOverride = true;
    private static boolean rpmOverride = true;
    private static boolean belt = true;
    private static boolean easterEggTab = true;
    private static boolean createCompat = true;
    private static boolean safeMode = false;

    private FeatureFlags() {
    }

    /** Auto-fire magazine behaviour (tags, bolt skip, capacity/RPM overrides, animations). */
    public static boolean autoFire() {
        return autoFire && !safeMode;
    }

    /** 30/45/60 magazine capacity override. */
    public static boolean capacityOverride() {
        return capacityOverride && autoFire() && !safeMode;
    }

    /** 400/600/1200 RPM override. */
    public static boolean rpmOverride() {
        return rpmOverride && autoFire() && !safeMode;
    }

    /** Ammo belt auto-feeding. */
    public static boolean belt() {
        return belt && !safeMode;
    }

    /** Show the easter-egg creative tab contents. */
    public static boolean easterEggTab() {
        return easterEggTab;
    }

    /** Create deployer compatibility. */
    public static boolean createCompat() {
        return createCompat && !safeMode;
    }

    /** Master switch: all cross-mod behaviour off, items and machines still work. */
    public static boolean safeMode() {
        return safeMode;
    }

    public static void load() {
        Path file = file();
        try {
            if (!Files.exists(file)) {
                save();
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            autoFire = bool(root, "autoFire", autoFire);
            capacityOverride = bool(root, "capacityOverride", capacityOverride);
            rpmOverride = bool(root, "rpmOverride", rpmOverride);
            belt = bool(root, "belt", belt);
            easterEggTab = bool(root, "easterEggTab", easterEggTab);
            createCompat = bool(root, "createCompat", createCompat);
            safeMode = bool(root, "safeMode", safeMode);
            // Persist new keys and comments for configs written by older versions.
            save();
        } catch (Exception exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to read feature flags, using defaults", exception);
        }
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        return root.has(key) ? root.get(key).getAsBoolean() : fallback;
    }

    private static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("_comment_cn",
                    "autoFire: 自动击发弹匣总开关; capacityOverride: 弹匣容量覆盖(30/45/60); rpmOverride: 射速覆盖(400/600/1200); belt: 自动供弹弹链; easterEggTab: 彩蛋创造分类; createCompat: Create 部署器兼容; safeMode: 总开关，关掉所有对其它模组的行为改动(物品与机器仍可用). 修改后执行 /taczammobox reload");
            root.addProperty("_comment_en",
                    "autoFire: auto-fire magazine master switch; capacityOverride: magazine capacity override; rpmOverride: RPM override; belt: auto-feeding ammo belt; easterEggTab: easter-egg creative tab; createCompat: Create deployer compatibility; safeMode: disable all cross-mod behaviour (items and machines still work). Run /taczammobox reload after editing.");

            JsonArray examplesCn = new JsonArray();
            examplesCn.add("TACZ 更新后弹匣行为异常: \"safeMode\": true 或 \"autoFire\": false");
            examplesCn.add("只想关射速覆盖: \"rpmOverride\": false");
            examplesCn.add("服主不想要弹链: \"belt\": false");
            root.add("_examples_cn", examplesCn);

            JsonArray examplesEn = new JsonArray();
            examplesEn.add("TACZ update broke the magazine: \"safeMode\": true or \"autoFire\": false");
            examplesEn.add("Keep vanilla RPM: \"rpmOverride\": false");
            examplesEn.add("Disable the belt: \"belt\": false");
            root.add("_examples_en", examplesEn);

            root.addProperty("autoFire", autoFire);
            root.addProperty("capacityOverride", capacityOverride);
            root.addProperty("rpmOverride", rpmOverride);
            root.addProperty("belt", belt);
            root.addProperty("easterEggTab", easterEggTab);
            root.addProperty("createCompat", createCompat);
            root.addProperty("safeMode", safeMode);
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to save feature flags", exception);
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("taczammobox").resolve("features.json");
    }

    /** Human readable snapshot for the startup log. */
    public static String describe() {
        return "autoFire=" + autoFire() + ", capacity=" + capacityOverride() + ", rpm=" + rpmOverride()
                + ", belt=" + belt() + ", easterEgg=" + easterEggTab() + ", create=" + createCompat()
                + ", safeMode=" + safeMode;
    }
}
