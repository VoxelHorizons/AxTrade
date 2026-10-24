package com.artillexstudios.axtrade.utils;

import java.util.Locale;

/**
 * Keep the legacy sign editor on versions without Paper's native Dialog API.
 * Mode 'dialog' requests native UI but still safely falls back if unavailable.
 */
public final class CurrencyInputMode {
    private CurrencyInputMode() {}

    public static boolean useDialog(String mode, boolean supported) {
        if (!supported) return false;
        String normalized = mode == null ? "auto" : mode.trim().toLowerCase(Locale.ROOT);
        return "auto".equals(normalized) || "dialog".equals(normalized);
    }
}
