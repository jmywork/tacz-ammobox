package com.taczammobox.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.taczammobox.TaczAmmoBoxMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime toggle, persisted in {@code config/taczammobox/settings.json} and
 * changed with {@code /taczammobox waste <true|false>}.
 *
 * <p>One shared switch: when enabled, both a single box and a crate may be
 * poured partially and the remainder is discarded. When disabled, the source
 * is only consumed if the ammo box has room for the whole package.</p>
 */
public final class WasteSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DEFAULT_TICKS = 5;
    public static final List<String> BOX_PRIORITY_OPTIONS =
            List.of("main_hand", "inventory", "curios", "open_container");

    private static boolean waste = true;
    private static boolean autoRefill = true;
    private static boolean oneKeyPack = true;
    private static int packerTicks = DEFAULT_TICKS;
    private static int loaderTicks = DEFAULT_TICKS;
    private static int boxStackSize = 64;
    private static int crateStackSize = 64;
    private static String capacityRounding = "round";
    private static List<String> boxPriority = new ArrayList<>(BOX_PRIORITY_OPTIONS);
    private static final Map<String, Integer> BOX_CAPACITY = new HashMap<>();
    private static final Map<String, Double> BOX_MULTIPLIER = new HashMap<>();

    private WasteSettings() {
    }

    public static boolean waste() {
        return waste;
    }

    public static void setWaste(boolean value) {
        waste = value;
        save();
    }

    public static boolean autoRefill() {
        return autoRefill;
    }

    public static void setAutoRefill(boolean value) {
        autoRefill = value;
        save();
    }

    public static boolean oneKeyPack() {
        return oneKeyPack;
    }

    public static void setOneKeyPack(boolean value) {
        oneKeyPack = value;
        save();
    }

    public static int boxStackSize() {
        return boxStackSize;
    }

    public static int crateStackSize() {
        return crateStackSize;
    }

    public static int packerTicks() {
        return packerTicks;
    }

    public static int loaderTicks() {
        return loaderTicks;
    }

    public static List<String> boxPriority() {
        return List.copyOf(boxPriority);
    }

    /** Parse a comma/space separated order; all four groups are required exactly once. */
    public static boolean setBoxPriority(String raw) {
        List<String> parsed = new ArrayList<>();
        for (String token : raw.toLowerCase().split("[,\\s]+")) {
            if (!token.isBlank()) {
                parsed.add(token);
            }
        }
        Set<String> unique = new HashSet<>(parsed);
        if (parsed.size() != BOX_PRIORITY_OPTIONS.size()
                || !unique.containsAll(BOX_PRIORITY_OPTIONS)) {
            return false;
        }
        boxPriority = parsed;
        save();
        return true;
    }

    /** Client-side values from the server. Does not write the local file. */
    public static void applyFromServer(boolean wasteValue, int packer, int loader) {
        waste = wasteValue;
        packerTicks = clampTicks(packer);
        loaderTicks = clampTicks(loader);
    }

    /**
     * Optional per-item capacity override for third-party ammo boxes whose
     * capacity is not TACZ's stackSize * AMMO_BOX_STACK_SIZE * (level + 1).
     * Key accepts {@code item_id} or {@code item_id#level}.
     */
    public static Integer boxCapacity(String itemId, int level) {
        return boxCapacity(itemId, level, null);
    }

    /**
     * Lookup order: exact ammo + level, then level for all ammo, then item-wide.
     * TACZ's normal capacity also varies by the ammo's stack_size, so the
     * {@code item#level#ammo} form is the safest override for standard boxes.
     */
    public static Integer boxCapacity(String itemId, int level, ResourceLocation ammoId) {
        if (ammoId != null) {
            Integer exact = BOX_CAPACITY.get(itemId + "#" + level + "#" + ammoId);
            if (exact != null) {
                return exact;
            }
        }
        Integer levelOverride = BOX_CAPACITY.get(itemId + "#" + level);
        return levelOverride != null ? levelOverride : BOX_CAPACITY.get(itemId);
    }

    public static Double boxCapacityMultiplier(String itemId, int level, ResourceLocation ammoId) {
        if (ammoId != null) {
            Double exact = BOX_MULTIPLIER.get(itemId + "#" + level + "#" + ammoId);
            if (exact != null) {
                return exact;
            }
        }
        Double levelOverride = BOX_MULTIPLIER.get(itemId + "#" + level);
        return levelOverride != null ? levelOverride : BOX_MULTIPLIER.get(itemId);
    }

    public static String capacityRounding() {
        return capacityRounding;
    }

    public static int roundCapacity(double value) {
        return switch (capacityRounding) {
            case "floor" -> (int) Math.floor(value);
            case "ceil" -> (int) Math.ceil(value);
            default -> (int) Math.round(value);
        };
    }

    public static boolean setCapacityRounding(String mode) {
        if (!mode.equals("round") && !mode.equals("floor") && !mode.equals("ceil")) {
            return false;
        }
        capacityRounding = mode;
        save();
        return true;
    }

    public static void recordDetectedMultiplier(String key, double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier <= 0 || BOX_MULTIPLIER.containsKey(key)) {
            return;
        }
        BOX_MULTIPLIER.put(key, multiplier);
        save();
        TaczAmmoBoxMod.LOGGER.info("Wrote detected ammo box multiplier {} = {}", key, multiplier);
    }

    public static java.util.Set<String> capacityKeys() {
        return java.util.Set.copyOf(BOX_CAPACITY.keySet());
    }

    public static java.util.Set<String> multiplierKeys() {
        return java.util.Set.copyOf(BOX_MULTIPLIER.keySet());
    }

    /** Persist an automatically detected custom capacity so the player can inspect/edit it. */
    public static void recordDetectedCapacity(String key, int capacity) {
        if (capacity <= 0 || BOX_CAPACITY.containsKey(key)) {
            return;
        }
        BOX_CAPACITY.put(key, capacity);
        save();
        TaczAmmoBoxMod.LOGGER.info("Wrote detected ammo box capacity {} = {}", key, capacity);
    }

    public static void setBoxCapacity(String key, int capacity) {
        BOX_CAPACITY.put(key, capacity);
        save();
    }

    public static void setBoxCapacityMultiplier(String key, double multiplier) {
        BOX_MULTIPLIER.put(key, multiplier);
        save();
    }

    public static boolean removeBoxCapacity(String key) {
        boolean changed = BOX_CAPACITY.remove(key) != null;
        changed |= BOX_MULTIPLIER.remove(key) != null;
        if (changed) {
            save();
        }
        return changed;
    }

    public static int removeBoxCapacitiesForItem(String itemId) {
        int before = BOX_CAPACITY.size() + BOX_MULTIPLIER.size();
        BOX_CAPACITY.keySet().removeIf(key -> key.equals(itemId) || key.startsWith(itemId + "#"));
        BOX_MULTIPLIER.keySet().removeIf(key -> key.equals(itemId) || key.startsWith(itemId + "#"));
        int removed = before - BOX_CAPACITY.size() - BOX_MULTIPLIER.size();
        if (removed > 0) {
            save();
        }
        return removed;
    }

    public static Map<String, Integer> boxCapacities() {
        return Map.copyOf(BOX_CAPACITY);
    }

    public static Map<String, Double> boxMultipliers() {
        return Map.copyOf(BOX_MULTIPLIER);
    }

    public static void load() {
        Path file = file();
        try {
            if (!Files.exists(file)) {
                save();
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("waste")) {
                waste = root.get("waste").getAsBoolean();
            } else if (root.has("crateWaste")) {
                // Migrate the old split setting: the crate value was the useful default.
                waste = root.get("crateWaste").getAsBoolean();
            } else if (root.has("boxWaste")) {
                waste = root.get("boxWaste").getAsBoolean();
            }
            packerTicks = clampTicks(root.has("packerTicks") ? root.get("packerTicks").getAsInt() : DEFAULT_TICKS);
            loaderTicks = clampTicks(root.has("loaderTicks") ? root.get("loaderTicks").getAsInt() : DEFAULT_TICKS);
            if (root.has("autoRefill")) {
                autoRefill = root.get("autoRefill").getAsBoolean();
            }
            if (root.has("oneKeyPack")) {
                oneKeyPack = root.get("oneKeyPack").getAsBoolean();
            }
            boxStackSize = clampStack(root.has("boxStackSize")
                    ? root.get("boxStackSize").getAsInt() : 64);
            crateStackSize = clampStack(root.has("crateStackSize")
                    ? root.get("crateStackSize").getAsInt() : 64);
            if (root.has("capacityRounding")) {
                String mode = root.get("capacityRounding").getAsString();
                if (mode.equals("round") || mode.equals("floor") || mode.equals("ceil")) {
                    capacityRounding = mode;
                }
            }
            if (root.has("boxPriority") && root.get("boxPriority").isJsonArray()) {
                List<String> parsed = new ArrayList<>();
                for (var element : root.getAsJsonArray("boxPriority")) {
                    String token = element.getAsString();
                    if (BOX_PRIORITY_OPTIONS.contains(token) && !parsed.contains(token)) {
                        parsed.add(token);
                    }
                }
                if (!parsed.isEmpty()) {
                    for (String option : BOX_PRIORITY_OPTIONS) {
                        if (!parsed.contains(option)) {
                            parsed.add(option);
                        }
                    }
                    boxPriority = parsed;
                }
            }
            BOX_CAPACITY.clear();
            BOX_MULTIPLIER.clear();
            if (root.has("ammoBoxCapacity") && root.get("ammoBoxCapacity").isJsonObject()) {
                for (var entry : root.getAsJsonObject("ammoBoxCapacity").entrySet()) {
                    try {
                        int capacity = entry.getValue().getAsInt();
                        if (capacity > 0) {
                            BOX_CAPACITY.put(entry.getKey(), capacity);
                        }
                    } catch (RuntimeException ignored) {
                        // Ignore malformed single entries; the rest still load.
                    }
                }
            }
            if (root.has("ammoBoxCapacityMultiplier") && root.get("ammoBoxCapacityMultiplier").isJsonObject()) {
                for (var entry : root.getAsJsonObject("ammoBoxCapacityMultiplier").entrySet()) {
                    try {
                        double multiplier = entry.getValue().getAsDouble();
                        if (Double.isFinite(multiplier) && multiplier > 0) {
                            BOX_MULTIPLIER.put(entry.getKey(), multiplier);
                        }
                    } catch (RuntimeException ignored) {
                        // Ignore malformed single entries; the rest still load.
                    }
                }
            }
            // Persist the bilingual comments and any newly added keys for older configs.
            save();
        } catch (Exception exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to read waste settings, using defaults", exception);
        }
    }

    private static int clampTicks(int value) {
        return Math.max(1, Math.min(200, value));
    }

    private static int clampStack(int value) {
        return Math.max(1, Math.min(64, value));
    }

    private static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("_comment_cn",
                    "waste: true=盒/箱装不满时消耗并浪费剩余, false=必须整组/整包装得下. autoRefill: 主手用完后是否自动从背包补同类物品. oneKeyPack: 是否允许 H/一键打包. boxStackSize/crateStackSize: 盒/箱最大堆叠, 1-64. boxPriority: 装填目标搜索顺序 main_hand/inventory/curios/open_container. packerTicks/loaderTicks: 每次操作耗时(tick), 20 tick = 1 秒, 范围 1-200. capacityRounding: 小数倍率取整方式 round/floor/ceil. ammoBoxCapacity: 绝对容量覆盖. ammoBoxCapacityMultiplier: 容量倍率覆盖. 键为 item_id / item_id#level / item_id#level#ammo.");
            root.addProperty("_comment_en",
                    "waste: true=consume a box even when it only partially fits and discard the rest, false=require the whole package to fit. autoRefill: pull a matching item back to the main hand after a fill. oneKeyPack: allow H/one-key packing. boxStackSize/crateStackSize: maximum box/crate stack size, 1-64. boxPriority: target search order main_hand/inventory/curios/open_container. packerTicks/loaderTicks: ticks per operation, 20 ticks = 1 second, range 1-200. capacityRounding: round/floor/ceil for fractional multipliers. ammoBoxCapacity: absolute capacity overrides. ammoBoxCapacityMultiplier: capacity multipliers. Keys are item_id / item_id#level / item_id#level#ammo.");

            JsonArray examplesCn = new JsonArray();
            examplesCn.add("把所有 TACZ 弹药盒容量改成 2 倍: \"ammoBoxCapacityMultiplier\": { \"tacz:ammo_box\": 2.0 }");
            examplesCn.add("只改铁盒倍率: { \"tacz:ammo_box#0\": 2.0 }");
            examplesCn.add("固定容量(不随弹种变化): \"ammoBoxCapacity\": { \"modid:custom_box\": 1080 }");
            examplesCn.add("小数倍率取整: \"capacityRounding\": \"floor\"");
            examplesCn.add("关闭主手自动补货: \"autoRefill\": false");
            examplesCn.add("关闭一键打包: \"oneKeyPack\": false");
            examplesCn.add("盒子/箱子堆叠 64: \"boxStackSize\": 64, \"crateStackSize\": 64");
            examplesCn.add("装填优先主手，然后当前容器: \"boxPriority\": [\"main_hand\", \"open_container\", \"inventory\", \"curios\"]");
            root.add("_examples_cn", examplesCn);

            JsonArray examplesEn = new JsonArray();
            examplesEn.add("Double all TACZ ammo box capacity: \"ammoBoxCapacityMultiplier\": { \"tacz:ammo_box\": 2.0 }");
            examplesEn.add("Iron box only: { \"tacz:ammo_box#0\": 2.0 }");
            examplesEn.add("Fixed capacity independent of ammo: \"ammoBoxCapacity\": { \"modid:custom_box\": 1080 }");
            examplesEn.add("Fractional rounding: \"capacityRounding\": \"floor\"");
            examplesEn.add("Disable main-hand auto refill: \"autoRefill\": false");
            examplesEn.add("Disable one-key packing: \"oneKeyPack\": false");
            examplesEn.add("Box/crate stack size 64: \"boxStackSize\": 64, \"crateStackSize\": 64");
            examplesEn.add("Prefer main hand, then the open container: \"boxPriority\": [\"main_hand\", \"open_container\", \"inventory\", \"curios\"]");
            root.add("_examples_en", examplesEn);

            root.addProperty("waste", waste);
            root.addProperty("autoRefill", autoRefill);
            root.addProperty("oneKeyPack", oneKeyPack);
            root.addProperty("boxStackSize", boxStackSize);
            root.addProperty("crateStackSize", crateStackSize);
            root.addProperty("packerTicks", packerTicks);
            root.addProperty("loaderTicks", loaderTicks);
            root.addProperty("capacityRounding", capacityRounding);
            JsonArray priority = new JsonArray();
            boxPriority.forEach(priority::add);
            root.add("boxPriority", priority);
            JsonObject capacities = new JsonObject();
            BOX_CAPACITY.forEach(capacities::addProperty);
            root.add("ammoBoxCapacity", capacities);
            JsonObject multipliers = new JsonObject();
            BOX_MULTIPLIER.forEach(multipliers::addProperty);
            root.add("ammoBoxCapacityMultiplier", multipliers);
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to save waste settings", exception);
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("taczammobox").resolve("settings.json");
    }
}
