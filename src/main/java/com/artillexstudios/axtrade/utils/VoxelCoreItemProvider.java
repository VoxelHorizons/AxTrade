package com.artillexstudios.axtrade.utils;

import com.artillexstudios.axtrade.AxTrade;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional runtime bridge to registered VoxelCore items (not merely item models).
 * Uses VoxelCore's ItemManager.createItem(ContentID) public API when available.
 */
public final class VoxelCoreItemProvider {
    private static final Set<String> warned = ConcurrentHashMap.newKeySet();

    private VoxelCoreItemProvider() {}

    @Nullable
    public static ItemStack create(String identifier) {
        Plugin voxelCore = Bukkit.getPluginManager().getPlugin("VoxelCore");
        if (voxelCore == null || !voxelCore.isEnabled()) {
            warn(identifier, "VoxelCore is not enabled");
            return null;
        }
        if (identifier == null || identifier.isBlank()) return null;

        try {
            // Get ItemManager from the live VoxelCore instance. The plugin uses
            // a separate classloader, so load ContentID through that instance.
            Object manager = voxelCore.getClass().getMethod("getItemManager").invoke(voxelCore);
            if (manager == null) {
                warn(identifier, "ItemManager has not initialized");
                return null;
            }
            Class<?> contentId = Class.forName("org.voxelhorizons.content.ContentID", true,
                    voxelCore.getClass().getClassLoader());
            Object id = contentId.getMethod("parse", String.class, String.class)
                    .invoke(null, identifier, "voxel");
            Method create = manager.getClass().getMethod("createItem", contentId);
            Object created = create.invoke(manager, id);
            if (created instanceof ItemStack item) {
                return item.clone();
            }
            warn(identifier, "VoxelCore returned an invalid item");
        } catch (ReflectiveOperationException | LinkageError | SecurityException | IllegalArgumentException error) {
            Throwable reason = error instanceof InvocationTargetException invocation
                    && invocation.getCause() != null ? invocation.getCause() : error;
            warn(identifier, reason.getMessage());
        }
        return null;
    }

    private static void warn(String identifier, String detail) {
        if (warned.add(identifier == null ? "(null)" : identifier)) {
            AxTrade.getInstance().getLogger().warning("Unable to resolve VoxelCore GUI item '"
                    + identifier + "': " + detail + ". Use material as a fallback if desired.");
        }
    }
}
