package com.artillexstudios.axtrade.utils;

import java.util.List;
import java.util.Map;

/**
 * Standalone regression test: run with plain javac/java, no Bukkit runtime.
 * The real runtime adapters are tested manually on a server with PAPI/VoxelCore.
 */
public final class GuiPlaceholderTextRegression {
    private GuiPlaceholderTextRegression() {}

    public static void main(String[] args) {
        Map<String, String> trade = Map.of(
                "%player%", "Partner",
                "%partner-name%", "Partner",
                "%amount%", "200",
                "%own-status%", "&aReady"
        );
        String title = GuiPlaceholderText.resolve(
                ":offset_-8::trade_menu:&f%player% %player_name% %own-status%",
                trade,
                line -> line.replace("%player_name%", "Viewer"),
                line -> line.replace(":offset_-8:", "SPACE").replace(":trade_menu:", "GLYPH"));
        expect("SPACEGLYPH&fPartner Viewer &aReady", title);

        String lore = GuiPlaceholderText.resolve(
                ":shop_coin:&f%amount% / %vault_eco_balance_commas%",
                trade,
                line -> line.replace("%vault_eco_balance_commas%", "1,000"),
                line -> line.replace(":shop_coin:", "COIN"));
        expect("COIN&f200 / 1,000", lore);

        List<String> lines = GuiPlaceholderText.resolveLines(
                List.of("%partner-name%", "%amount%", "%player_name%"),
                trade, line -> line.replace("%player_name%", "Viewer"), null);
        if (!List.of("Partner", "200", "Viewer").equals(lines)) {
            throw new AssertionError("Unexpected lore: " + lines);
        }

        expect("%placeholder_without_plugin%",
                GuiPlaceholderText.resolve("%placeholder_without_plugin%", Map.of(), null, null));
        expect(null, GuiPlaceholderText.resolve(null, trade, null, null));
    }

    private static void expect(String expected, String actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
