package com.artillexstudios.axtrade.utils;

/**
 * Resolves a GUI confirmation control's stack size without inventing state
 * dependent text or mutating the configured item metadata.
 */
public final class ConfirmationItemPolicy {
    private ConfirmationItemPolicy() {}

    /**
     * @return -1 to keep the ItemBuilder's explicitly configured amount,
     *         otherwise a stack count between 1 and 64.
     */
    public static int amount(boolean showCountdown, boolean hasConfiguredAmount, int remainingSeconds) {
        if (showCountdown) return Math.max(1, Math.min(64, remainingSeconds));
        return hasConfiguredAmount ? -1 : 1;
    }
}
