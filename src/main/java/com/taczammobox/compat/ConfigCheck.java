package com.taczammobox.compat;

import com.taczammobox.AmmoType;
import com.taczammobox.registry.AmmoTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Stable, registry-relevant snapshot used by the login config check. */
public final class ConfigCheck {
    private ConfigCheck() {
    }

    public static List<String> snapshot() {
        List<String> entries = new ArrayList<>();
        for (AmmoType type : AmmoTypes.all()) {
            entries.add(type.ammoId() + "|" + type.itemName());
        }
        entries.sort(String::compareTo);
        return entries;
    }

    public static List<String> missing(List<String> server, List<String> client) {
        Set<String> present = new HashSet<>(client);
        List<String> result = new ArrayList<>();
        for (String entry : server) {
            if (!present.contains(entry)) {
                result.add(entry);
            }
        }
        return result;
    }

    public static List<String> extra(List<String> server, List<String> client) {
        Set<String> present = new HashSet<>(server);
        List<String> result = new ArrayList<>();
        for (String entry : client) {
            if (!present.contains(entry)) {
                result.add(entry);
            }
        }
        return result;
    }
}
