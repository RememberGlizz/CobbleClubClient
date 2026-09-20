/*
 * Decompiled with CFR 0.152.
 */
package com.cobbleclub.server.util;

import java.util.Locale;

public final class CrateKeyIds {
    private CrateKeyIds() {
    }

    public static String canonical(String id) {
        if (id == null) {
            return "";
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        if (normalized.endsWith("_key") && normalized.length() > 4) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }
}

