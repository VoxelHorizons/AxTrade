package com.artillexstudios.axtrade.utils;

import java.util.Map;

/** Determines the optional VoxelCore render source without affecting vanilla materials. */
public final class VoxelItemReference {
    private VoxelItemReference() {}

    /**
     * Explicit vitem takes priority, then the older voxelcore key, then
     * the backwards-compatible material: voxel:... shorthand.
     */
    public static String select(Map<String, Object> settings) {
        Object vitem = settings.get("vitem");
        if (vitem instanceof String value && !value.isBlank()) return value.trim();
        Object legacy = settings.get("voxelcore");
        if (legacy instanceof String value && !value.isBlank()) return value.trim();
        Object material = settings.get("material");
        if (material instanceof String value && value.startsWith("voxel:")) return value;
        return null;
    }

    /** VoxelCore registered ContentIDs cannot contain '/', but pack model paths can. */
    public static boolean isModelAsset(String identifier) {
        if (identifier == null) return false;
        int separator = identifier.indexOf(':');
        String path = separator < 0 ? identifier : identifier.substring(separator + 1);
        return path.contains("/");
    }

    public static String modelKey(String identifier) {
        return identifier.indexOf(':') < 0 ? "voxel:" + identifier : identifier;
    }
}
