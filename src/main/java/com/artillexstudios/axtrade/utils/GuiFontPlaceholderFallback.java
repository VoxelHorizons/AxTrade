package com.artillexstudios.axtrade.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.UnaryOperator;

/**
 * Translates unresolved VoxelCore :name: and :namespace/name: aliases through
 * its built-in PlaceholderAPI expansion when direct API access is unavailable.
 *
 * An unknown alias remains unchanged, never rendered as a literal %...%.
 */
public final class GuiFontPlaceholderFallback {
    private static final Pattern ALIAS = Pattern.compile(":([a-z0-9_.-]+(?:/[a-z0-9_.-]+)?):");

    private GuiFontPlaceholderFallback() {}

    public static String resolve(String input, UnaryOperator<String> placeholderApi) {
        if (input == null || placeholderApi == null || input.indexOf(':') < 0) return input;
        Matcher matcher = ALIAS.matcher(input);
        StringBuffer result = new StringBuffer(input.length());
        while (matcher.find()) {
            String query = "%voxelcore_font_" + matcher.group(1) + "%";
            String value = placeholderApi.apply(query);
            // PlaceholderAPI returns an untouched token for missing expansions.
            String replacement = value == null || value.isEmpty() || value.equals(query)
                    ? matcher.group(0) : value;
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
