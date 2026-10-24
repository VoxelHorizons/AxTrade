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


        // Config prefixes and lang messages share the ordered parser.
        String chat = GuiPlaceholderText.resolve(
                ":branding_icon_small: &7[%player_name%] &fTrade with %player% for %amount%",
                trade,
                line -> line.replace("%player_name%", "Viewer"),
                line -> line.replace(":branding_icon_small:", "ICON"));
        expect("ICON &7[Viewer] &fTrade with Partner for 200", chat);

        // Hover labels render with the recipient context, without changing click actions.
        String hover = GuiPlaceholderText.resolve(
                "Click to accept %player%'s trade (%player_name%)", trade,
                line -> line.replace("%player_name%", "Viewer"), null);
        expect("Click to accept Partner's trade (Viewer)", hover);
        expect("Trading %player_name%", GuiPlaceholderText.resolve(
                "Trading %player_name%", Map.of(), null, null));

        // Both own/partner controls use one item by default, even after
        // the countdown begins at trade-confirm-seconds: 5.
        expectInt(1, ConfirmationItemPolicy.amount(false, false, 5));
        expectInt(1, ConfirmationItemPolicy.amount(false, false, 1));
        // An explicitly configured amount must not be silently overwritten.
        expectInt(-1, ConfirmationItemPolicy.amount(false, true, 5));
        // Only an opted-in control shows remaining seconds on its stack.
        expectInt(5, ConfirmationItemPolicy.amount(true, false, 5));
        expectInt(1, ConfirmationItemPolicy.amount(true, false, 0));
        expectInt(64, ConfirmationItemPolicy.amount(true, false, 100));

        // New vitem is highest priority; older config forms still work.
        expect("voxel:tractor", VoxelItemReference.select(Map.of(
                "vitem", "voxel:tractor", "voxelcore", "voxel:old", "material", "GOLD_NUGGET")));
        expect("voxel:ui-blank", VoxelItemReference.select(Map.of(
                "voxelcore", "voxel:ui-blank", "material", "BARRIER")));
        expect("voxel:chair", VoxelItemReference.select(Map.of("material", "voxel:chair")));
        expect(null, VoxelItemReference.select(Map.of("material", "RED_CONCRETE")));
        if (!VoxelItemReference.isModelAsset("voxel:ui/blank")
                || VoxelItemReference.isModelAsset("voxel:tractor")) {
            throw new AssertionError("Raw item-model asset detection failed");
        }
        expect("voxel:ui/blank", VoxelItemReference.modelKey("ui/blank"));

        // Fallback must expand precisely the colon-style VoxelCore aliases
        // shown in Paper's AxTrade title screenshot, including negative offsets.
        String legacy = GuiFontPlaceholderFallback.resolve(
                ":offset_-8::generic_54_top:Trading with Partner",
                token -> switch (token) {
                    case "%voxelcore_font_offset_-8%" -> "\\uF800";
                    case "%voxelcore_font_generic_54_top%" -> "\\uF801";
                    default -> token;
                });
        expect("\\uF800\\uF801Trading with Partner", legacy);
        expect("unknown :not_a_known_glyph:!",
                GuiFontPlaceholderFallback.resolve("unknown :not_a_known_glyph:!", token -> token));
        expect(":offset_-8:", GuiFontPlaceholderFallback.resolve(":offset_-8:", null));

        expect("%placeholder_without_plugin%",
                GuiPlaceholderText.resolve("%placeholder_without_plugin%", Map.of(), null, null));
        expect(null, GuiPlaceholderText.resolve(null, trade, null, null));
    }

    private static void expectInt(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected stack amount " + expected + " but got " + actual);
        }
    }

    private static void expect(String expected, String actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
