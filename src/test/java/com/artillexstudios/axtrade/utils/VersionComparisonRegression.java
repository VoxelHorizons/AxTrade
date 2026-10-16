package com.artillexstudios.axtrade.utils;

/** Headless regression coverage for AxTrade and VoxelHorizons version labels. */
public final class VersionComparisonRegression {
    private VersionComparisonRegression() {}

    public static void main(String[] args) {
        expect(true, VersionComparison.isNewer("1.28.3", "1.28.2-voxel-gui-test"));
        expect(false, VersionComparison.isNewer("1.28.2", "1.28.2-voxel-gui-test"));
        expect(false, VersionComparison.isNewer("1.27.9", "1.28.2-voxel-gui-test"));
        expect(true, VersionComparison.isNewer("v2.0.0", "1.28.2"));
        expect(false, VersionComparison.isNewer(null, "1.28.2-voxel-gui-test"));
        expect(false, VersionComparison.isNewer("This is not a version", "1.28.2"));
        expect(false, VersionComparison.isNewer("1.28.2", "unknown"));
        expect(false, VersionComparison.isNewer("999999999999.0.0", "1.28.2"));
        expect(false, VersionComparison.isNewer("1.28", "1.28.2"));
    }

    private static void expect(boolean expected, boolean actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
