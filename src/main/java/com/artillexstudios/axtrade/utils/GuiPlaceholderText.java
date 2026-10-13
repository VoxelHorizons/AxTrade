package com.artillexstudios.axtrade.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Pure ordered placeholder pipeline shared by all AxTrade GUI content.
 * The trade's own %...% tokens are substituted before PlaceholderAPI and
 * VoxelCore aliases so neither external parser consumes them prematurely.
 */
public final class GuiPlaceholderText {
    private GuiPlaceholderText() {}

    public static String resolve(String input, Map<String, String> replacements,
                                 UnaryOperator<String> placeholderApi, UnaryOperator<String> voxelCore) {
        if (input == null) return null;
        String result = input;
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result = result.replace(entry.getKey(), entry.getValue());
                }
            }
        }
        if (placeholderApi != null && result.indexOf('%') >= 0) {
            result = placeholderApi.apply(result);
        }
        if (voxelCore != null && result.indexOf(':') >= 0) {
            result = voxelCore.apply(result);
        }
        return result;
    }

    public static List<String> resolveLines(List<String> lines, Map<String, String> replacements,
                                            UnaryOperator<String> placeholderApi, UnaryOperator<String> voxelCore) {
        if (lines == null) return null;
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) out.add(resolve(line, replacements, placeholderApi, voxelCore));
        return out;
    }
}
