package com.taczammobox.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tacz.guns.api.item.IAttachment;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.util.AmmoChainHelper;
import com.taczammobox.util.AutoFireTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
    private static boolean ammoChain = true;
    private static boolean ammoFactory = true;
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

    /** Feed belt: reload from boxed/crated ammo instead of TACZ ammo boxes. */
    public static boolean ammoChain() {
        return ammoChain && !safeMode;
    }

    /** Ammo assembler and its intermediate items. */
    public static boolean ammoFactory() {
        return ammoFactory && !safeMode;
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

    /**
     * Runtime-only shutdown of the auto-fire feature, used by the compatibility self check
     * when a TACZ update removed something we rely on. The file is left untouched so the
     * user can still see their configured value.
     */
    public static void disableAutoFire(String reason) {
        if (autoFire) {
            autoFire = false;
            capacityOverride = false;
            rpmOverride = false;
            TaczAmmoBoxMod.LOGGER.warn("[taczammobox] auto-fire disabled at runtime: {}", reason);
        }
    }

    /** Client-side apply of the server's values; never writes the local file. */
    public static void applyFromServer(boolean autoFireValue, boolean capacityOverrideValue,
            boolean rpmOverrideValue, boolean beltValue, boolean easterEggTabValue,
            boolean createCompatValue, boolean safeModeValue) {
        autoFire = autoFireValue;
        capacityOverride = capacityOverrideValue;
        rpmOverride = rpmOverrideValue;
        belt = beltValue;
        easterEggTab = easterEggTabValue;
        createCompat = createCompatValue;
        safeMode = safeModeValue;
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
            ammoChain = bool(root, "ammoChain", ammoChain);
            ammoFactory = bool(root, "ammoFactory", ammoFactory);
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
                    "autoFire: 自动击发弹匣总开关; capacityOverride: 弹匣容量覆盖(30/45/60); rpmOverride: 射速覆盖(400/600/1200); belt: 自动供弹弹链; easterEggTab: 彩蛋物品是否出现在创造栏; createCompat: Create 部署器兼容; safeMode: 总开关，关掉所有对其它模组的行为改动(物品与机器仍可用). 修改后执行 /taczammobox reload");
            root.addProperty("_comment_en",
                    "autoFire: auto-fire magazine master switch; capacityOverride: magazine capacity override; rpmOverride: RPM override; belt: auto-feeding ammo belt; easterEggTab: show the easter-egg items in the creative tab; createCompat: Create deployer compatibility; safeMode: disable all cross-mod behaviour (items and machines still work). Run /taczammobox reload after editing.");

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
            root.addProperty("ammoChain", ammoChain);
            root.addProperty("ammoFactory", ammoFactory);
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

    // ---------------------------------------------------------------------
    // Bridge for the mixins that are merged into TACZ classes.
    //
    // An injected body physically lives inside the target class, so every class it
    // references is resolved by TACZ's own module class loader. Going through this
    // single class (and catching Throwable in the hooks) keeps a refused lookup from
    // escalating into NoClassDefFoundError: the one behaviour goes quiet instead.
    // ---------------------------------------------------------------------

    /** Capacity of the tier magazine in {@code stack}, or {@code -1} when it is not one. */
    public static int tierCapacityFor(ItemStack stack) {
        AutoFireTier tier = AutoFireTier.of(stack);
        return tier == null ? -1 : tier.capacity();
    }

    /** Shot interval in milliseconds of the tier magazine in {@code stack}, or {@code -1}. */
    public static long tierIntervalFor(ItemStack stack) {
        AutoFireTier tier = AutoFireTier.of(stack);
        return tier == null ? -1L : tier.shootInterval();
    }

    /**
     * True when {@code stack} is one of our auto-fire magazines, either as a loose
     * attachment item or mounted on a gun.
     */
    public static boolean isTierMagazine(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof IAttachment attachment) {
            return AutoFireTier.of(attachment.getAttachmentId(stack)) != null;
        }
        return AutoFireTier.of(stack) != null;
    }

    /** Attachment ids of every tier, for the allow-attachment tag rewrite. */
    public static List<String> tierAttachmentIds() {
        List<String> ids = new ArrayList<>();
        for (AutoFireTier tier : AutoFireTier.all()) {
            ids.add(tier.id().toString());
        }
        return ids;
    }

    /** Feed belt lookup, kept here so the reload mixin only talks to this class. */
    /** Registry id of a stack, used by diagnostics. */
    public static String itemIdForLog(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static boolean chainHasReloadSource(Player player, ItemStack gunStack) {
        return ammoChain() && AmmoChainHelper.hasReloadSource(player, gunStack);
    }
}
