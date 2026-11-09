package com.taczammobox.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.taczammobox.TaczAmmoBoxMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The three auto-fire magazine tiers.
 *
 * <p>Capacity and RPM live in {@code config/taczammobox/autofire.json} so pack authors can
 * rebalance them without a code change. Defaults: 30/400, 45/600, 60/800.</p>
 */
public final class AutoFireTier {
    public static final int MAX_RPM = 1200;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<AutoFireTier> ALL = new ArrayList<>();

    private final String path;
    private final ResourceLocation id;
    private final int level;
    private final int capacity;
    private final int rpm;

    private AutoFireTier(String path, int level, int capacity, int rpm) {
        this.path = path;
        this.id = new ResourceLocation(TaczAmmoBoxMod.MOD_ID, path);
        this.level = level;
        this.capacity = capacity;
        this.rpm = rpm;
    }

    public ResourceLocation id() {
        return id;
    }

    public String path() {
        return path;
    }

    /** Extended magazine level, used for the tooltip and the refit diagram. */
    public int level() {
        return level;
    }

    public int capacity() {
        return capacity;
    }

    public int rpm() {
        return rpm;
    }

    /** Shoot interval in milliseconds. */
    public long shootInterval() {
        return 60000L / Math.max(1, rpm);
    }

    public static List<AutoFireTier> all() {
        return List.copyOf(ALL);
    }

    public static AutoFireTier of(ResourceLocation attachmentId) {
        if (attachmentId == null) {
            return null;
        }
        for (AutoFireTier tier : ALL) {
            if (tier.id.equals(attachmentId)) {
                return tier;
            }
        }
        return null;
    }

    /** Tier installed on a gun stack, or null when none of ours is mounted. */
    public static AutoFireTier of(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return null;
        }
        return of(gun.getAttachmentId(gunStack, AttachmentType.EXTENDED_MAG));
    }

    /** Client-side apply of the server's capacity/RPM values; never writes the local file. */
    public static void applyFromServer(int[] capacities, int[] rpms) {
        if (capacities.length != ALL.size() || rpms.length != ALL.size()) {
            return;
        }
        List<AutoFireTier> updated = new ArrayList<>();
        for (int index = 0; index < ALL.size(); index++) {
            AutoFireTier tier = ALL.get(index);
            updated.add(new AutoFireTier(tier.path, tier.level, clamp(capacities[index], 1, 2000),
                    clamp(rpms[index], 1, MAX_RPM)));
        }
        ALL.clear();
        ALL.addAll(updated);
    }

    /** Loads (and rewrites) the tier table; call before the gun pack is generated. */
    public static void init() {
        ALL.clear();
        Path file = file();
        try {
            if (Files.exists(file)) {
                JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray tiers = root.has("tiers") ? root.getAsJsonArray("tiers") : null;
                if (tiers != null) {
                    int index = 0;
                    for (JsonElement element : tiers) {
                        if (!element.isJsonObject()) {
                            continue;
                        }
                        JsonObject tier = element.getAsJsonObject();
                        String id = tier.has("id") ? tier.get("id").getAsString() : defaultPath(index);
                        int level = tier.has("level") ? tier.get("level").getAsInt() : index + 1;
                        int capacity = tier.has("capacity") ? tier.get("capacity").getAsInt() : defaultCapacity(index);
                        int rpm = tier.has("rpm") ? tier.get("rpm").getAsInt() : defaultRpm(index);
                        if (index < 3) {
                            ALL.add(new AutoFireTier(id, clamp(level, 1, 3), clamp(capacity, 1, 2000),
                                    clamp(rpm, 1, MAX_RPM)));
                        }
                        index++;
                    }
                }
            }
        } catch (Exception exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to read autofire.json, using defaults", exception);
        }
        if (ALL.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                ALL.add(new AutoFireTier(defaultPath(i), i + 1, defaultCapacity(i), defaultRpm(i)));
            }
        }
        save();
        TaczAmmoBoxMod.LOGGER.info("[taczammobox] auto-fire tiers: {}", describe());
    }

    private static String defaultPath(int index) {
        return index == 0 ? "auto_sear" : "auto_sear_" + (index + 1);
    }

    private static int defaultCapacity(int index) {
        return switch (index) {
            case 1 -> 45;
            case 2 -> 60;
            default -> 30;
        };
    }

    private static int defaultRpm(int index) {
        return switch (index) {
            case 1 -> 600;
            case 2 -> 800;
            default -> 400;
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("_comment_cn",
                    "三级自动击发弹匣的参数。capacity 弹容，rpm 射速(上限 1200)；level 影响 tooltip 与贴图（1-3）。修改后重启游戏生效，枪包在启动时生成。");
            root.addProperty("_comment_en",
                    "Auto-fire magazine tiers. capacity = rounds, rpm = fire rate (max 1200); level drives the tooltip and model (1-3). Restart the game after editing; the TACZ gun pack is generated at startup.");

            JsonArray tiers = new JsonArray();
            for (AutoFireTier tier : ALL) {
                JsonObject entry = new JsonObject();
                entry.addProperty("id", tier.path);
                entry.addProperty("level", tier.level);
                entry.addProperty("capacity", tier.capacity);
                entry.addProperty("rpm", tier.rpm);
                tiers.add(entry);
            }
            root.add("tiers", tiers);
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to save autofire.json", exception);
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("taczammobox").resolve("autofire.json");
    }

    public static String describe() {
        StringBuilder builder = new StringBuilder();
        for (AutoFireTier tier : ALL) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(tier.path).append('=').append(tier.capacity).append('r').append('/').append(tier.rpm);
        }
        return builder.toString();
    }
}
