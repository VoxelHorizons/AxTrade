package com.artillexstudios.axtrade.utils;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.utils.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ItemBuilderUtil {

    @NotNull
    public static ItemBuilder newBuilder(@NotNull Section section, @Nullable Player player) {
        return newBuilder(section, Map.of(), player);
    }

    @NotNull
    public static ItemBuilder newBuilder(@NotNull Section section, Map<String, String> replacements, @Nullable Player player) {
        // Resolve every GUI-visible item configuration field before AxAPI creates
        // the ItemStack. Its built-in name/lore parsing happens while creating the
        // builder, so post-processing only those two fields misses material,
        // model, texture, tooltip and all nested config values.
        return ItemBuilder.create(GuiPlaceholderResolver.resolveItemSection(player, section, replacements));
    }

    @NotNull
    @Contract("_ -> new")
    public static ItemBuilder newBuilder(@NotNull ItemStack itemStack) {
        return ItemBuilder.create(itemStack);
    }
}
