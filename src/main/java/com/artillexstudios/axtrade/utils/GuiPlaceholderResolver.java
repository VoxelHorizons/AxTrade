package com.artillexstudios.axtrade.utils;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Resolves all trade GUI text in a consistent order:
 * (1) internal trade tokens, (2) optional PlaceholderAPI with viewer context,
 * (3) optional VoxelCore UI font aliases/offsets.
 *
 * This bridge does not have a compile-time dependency on VoxelCore. It uses
 * VoxelCore's public TextPlaceholderService when that plugin is available.
 */
public final class GuiPlaceholderResolver {
    private static volatile VoxelCoreBridge voxelCoreBridge;

    private GuiPlaceholderResolver() {}

    public static String resolve(@Nullable Player viewer, String text) {
        return resolve(viewer, text, Map.of());
    }

    public static String resolve(@Nullable Player viewer, String text, Map<String, String> replacements) {
        if (text == null) return null;

        UnaryOperator<String> papi = null;
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papi = input -> PlaceholderAPI.setPlaceholders(viewer, input);
        }

        UnaryOperator<String> glyphs = null;
        Plugin voxelCore = Bukkit.getPluginManager().getPlugin("VoxelCore");
        if (voxelCore != null && voxelCore.isEnabled()) {
            VoxelCoreBridge bridge = voxelCoreBridge;
            if (bridge == null || bridge.plugin != voxelCore) {
                bridge = VoxelCoreBridge.connect(voxelCore);
                voxelCoreBridge = bridge;
            }
            final VoxelCoreBridge current = bridge;
            if (current != null && current.available()) {
                glyphs = current::resolve;
            } else if (papi != null) {
                // Older VoxelCore distributions can lack the public getter.
                // Use its existing %voxelcore_font_<name>% PAPI expansion,
                // including :offset_-8:, without needing a VoxelCore update.
                glyphs = input -> GuiFontPlaceholderFallback.resolve(input,
                        name -> PlaceholderAPI.setPlaceholders(viewer, name));
            }
        }

        return GuiPlaceholderText.resolve(text, replacements, papi, glyphs);
    }

    public static List<String> resolveLines(@Nullable Player viewer, List<String> lines,
                                            Map<String, String> replacements) {
        if (lines == null) return null;
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) out.add(resolve(viewer, line, replacements));
        return out;
    }

    /**
     * Resolve configuration values before the AxAPI ItemBuilder reads them.
     * This covers material/type, item-model, tooltip-style, texture, name,
     * lore, amount, custom-model-data and nested builder settings without
     * modifying the user's config or losing numeric/boolean YAML types.
     */
    public static Map<String, Object> resolveItemSection(Player viewer, Section section,
                                                          Map<String, String> replacements) {
        Map<String, Object> output = new LinkedHashMap<>();
        for (Object key : section.getKeys()) {
            String name = String.valueOf(key);
            output.put(name, resolveValue(viewer, section.get(name), replacements));
        }
        return output;
    }

    private static Object resolveValue(Player viewer, Object value, Map<String, String> replacements) {
        if (value instanceof String) {
            return resolve(viewer, (String) value, replacements);
        }
        if (value instanceof Section) {
            return resolveItemSection(viewer, (Section) value, replacements);
        }
        if (value instanceof Map<?, ?>) {
            Map<Object, Object> output = new LinkedHashMap<>();
            ((Map<?, ?>) value).forEach((key, child) ->
                    output.put(key, resolveValue(viewer, child, replacements)));
            return output;
        }
        if (value instanceof List<?>) {
            List<Object> output = new ArrayList<>(((List<?>) value).size());
            for (Object child : (List<?>) value) {
                output.add(resolveValue(viewer, child, replacements));
            }
            return output;
        }
        // Preserve ints, doubles, booleans, enums and null unchanged.
        return value;
    }

    private static final class VoxelCoreBridge {
        private final Plugin plugin;
        private final Object service;
        private final Method resolveMethod;

        private VoxelCoreBridge(Plugin plugin, Object service, Method resolveMethod) {
            this.plugin = plugin;
            this.service = service;
            this.resolveMethod = resolveMethod;
        }

        private static VoxelCoreBridge connect(Plugin plugin) {
            try {
                Object service;
                try {
                    // Preferred API on newer VoxelCore builds.
                    service = plugin.getClass().getMethod("getTextPlaceholderService").invoke(plugin);
                } catch (NoSuchMethodException missing) {
                    // Compatibility with previously deployed VoxelCore builds.
                    // The fallback is only used if the public accessor is absent.
                    Field field = plugin.getClass().getDeclaredField("textPlaceholderService");
                    field.setAccessible(true);
                    service = field.get(plugin);
                }
                if (service == null) {
                    throw new IllegalStateException("VoxelCore UI placeholder service has not initialized");
                }
                Method resolve = service.getClass().getMethod("resolve", String.class);
                return new VoxelCoreBridge(plugin, service, resolve);
            } catch (ReflectiveOperationException | LinkageError | SecurityException | IllegalStateException exception) {
                plugin.getLogger().warning("AxTrade could not access VoxelCore's UI resolver; "
                        + "using the PlaceholderAPI font fallback when available: " + exception.getMessage());
                return new VoxelCoreBridge(plugin, null, null);
            }
        }

        private boolean available() {
            return service != null && resolveMethod != null;
        }

        private String resolve(String input) {
            try {
                Object resolved = resolveMethod.invoke(service, input);
                return resolved instanceof String ? (String) resolved : input;
            } catch (IllegalAccessException | InvocationTargetException exception) {
                // A misconfigured font must not prevent trading or opening its GUI.
                return input;
            }
        }
    }
}
