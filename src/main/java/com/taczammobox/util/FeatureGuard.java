package com.taczammobox.util;

import com.taczammobox.TaczAmmoBoxMod;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * One-time failure guard for cross-mod hooks.
 *
 * <p>A single unexpected exception (a changed TACZ signature, a null stack, ...) should
 * disable only the feature that threw, log it once, and leave the rest of the mod and the
 * game running. Without this a broken hook turns into a log flood or a hard crash.</p>
 */
public final class FeatureGuard {
    private static final Set<String> DISABLED = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> REASONS = new ConcurrentHashMap<>();

    private FeatureGuard() {
    }

    public static boolean isDisabled(String feature) {
        return DISABLED.contains(feature);
    }

    /** Runs an action; on the first failure the feature is disabled and logged once. */
    public static void run(String feature, Runnable action) {
        if (DISABLED.contains(feature)) {
            return;
        }
        try {
            action.run();
        } catch (Throwable throwable) {
            disable(feature, throwable);
        }
    }

    /** Supplier variant that falls back to {@code fallback} when the feature is broken. */
    public static <T> T get(String feature, Supplier<T> supplier, T fallback) {
        if (DISABLED.contains(feature)) {
            return fallback;
        }
        try {
            return supplier.get();
        } catch (Throwable throwable) {
            disable(feature, throwable);
            return fallback;
        }
    }

    private static void disable(String feature, Throwable throwable) {
        if (DISABLED.add(feature)) {
            REASONS.put(feature, throwable.getClass().getSimpleName());
            TaczAmmoBoxMod.LOGGER.error(
                    "[taczammobox] Feature '{}' disabled after an unexpected error; the rest of the mod keeps running",
                    feature, throwable);
        }
    }

    /** Snapshot for /taczammobox doctor. */
    public static String describe() {
        if (DISABLED.isEmpty()) {
            return "all features healthy";
        }
        StringBuilder builder = new StringBuilder("disabled:");
        for (String name : DISABLED) {
            builder.append(' ').append(name).append('(').append(REASONS.getOrDefault(name, "?")).append(')');
        }
        return builder.toString();
    }
}
