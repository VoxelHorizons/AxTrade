package com.artillexstudios.axtrade.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares the numeric major/minor/patch components of release versions.
 * Accepts optional prefixed 'v' and build labels (such as -voxel-gui-test).
 * Malformed or out-of-range versions are treated as incomparable.
 */
public final class VersionComparison {
    private static final Pattern VERSION = Pattern.compile(
            "^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+][0-9A-Za-z.+-]+)?$");

    private VersionComparison() {}

    public static boolean isNewer(String latest, String current) {
        int[] left = numbers(latest);
        int[] right = numbers(current);
        if (left == null || right == null) return false;
        for (int i = 0; i < 3; i++) {
            if (left[i] > right[i]) return true;
            if (left[i] < right[i]) return false;
        }
        return false;
    }

    private static int[] numbers(String version) {
        if (version == null) return null;
        Matcher match = VERSION.matcher(version.trim());
        if (!match.matches()) return null;
        try {
            return new int[]{
                    Integer.parseInt(match.group(1)),
                    Integer.parseInt(match.group(2)),
                    Integer.parseInt(match.group(3))
            };
        } catch (NumberFormatException invalid) {
            return null;
        }
    }
}
