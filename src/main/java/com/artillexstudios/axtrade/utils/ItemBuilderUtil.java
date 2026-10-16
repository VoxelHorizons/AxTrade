package com.artillexstudios.axtrade.utils;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.utils.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.List;

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
        Map<String, Object> settings = GuiPlaceholderResolver.resolveItemSection(player, section, replacements);
        // Accept the underscore spelling that DeluxeMenus users commonly use,
        // while preserving AxAPI's documented item-model key.
        if (settings.containsKey("item_model") && !settings.containsKey("item-model")) {
            settings.put("item-model", settings.get("item_model"));
        }

        Object id = settings.get("voxelcore");
        if (id == null && settings.get("material") instanceof String material
                && material.startsWith("voxel:")) {
            // Accept the concise material: voxel:ui-blank form as well as
            // the explicit voxelcore: voxel:ui-blank key.
            id = material;
            settings.remove("material");
        }
        if (id instanceof String identifier && !identifier.isBlank()) {
            ItemStack registered = VoxelCoreItemProvider.create(identifier);
            if (registered != null) {
                return customizeRegisteredItem(registered, settings);
            }
            // Never create an AIR/null control with a click action. A visible
            // vanilla fallback can be configured, otherwise use a BARRIER.
            if (!settings.containsKey("material") && !settings.containsKey("type")) {
                settings.put("material", "BARRIER");
            }
        }
        return ItemBuilder.create(settings);
    }

    /** Override only fields explicitly configured; keep VoxelCore PDC/model/meta. */
    private static ItemBuilder customizeRegisteredItem(ItemStack item, Map<String, Object> settings) {
        ItemBuilder builder = ItemBuilder.create(item);
        if (settings.get("name") instanceof String name) builder.setName(name);
        if (settings.get("lore") instanceof List<?> lines) {
            builder.setLore(lines.stream().map(String::valueOf).toList());
        }
        if (settings.get("item-model") instanceof String model) builder.itemModel(model);
        if (settings.get("tooltip-style") instanceof String style) builder.tooltipStyle(style);
        if (settings.get("texture") instanceof String texture) builder.setTextureValue(texture);
        if (settings.get("glow") instanceof Boolean glow) builder.glow(glow);
        if (settings.get("unbreakable") instanceof Boolean unbreakable) builder.unbreakable(unbreakable);
        if (settings.get("color") instanceof String color) builder.setColor(color);
        Object amount = settings.get("amount");
        if (amount instanceof Number number) {
            builder.amount(Math.max(1, Math.min(64, number.intValue())));
        } else if (amount instanceof String number) {
            try { builder.amount(Math.max(1, Math.min(64, Integer.parseInt(number)))); }
            catch (NumberFormatException ignored) { /* keep the default VoxelCore quantity */ }
        }
        Object customModel = settings.get("custom-model-data");
        if (customModel instanceof Number number) builder.legacyModelData(number.intValue());
        if (customModel instanceof Map<?, ?> model) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> data = (Map<Object, Object>) model;
            builder.customModelData(data);
        }
        return builder;
    }

    @NotNull
    @Contract("_ -> new")
    public static ItemBuilder newBuilder(@NotNull ItemStack itemStack) {
        return ItemBuilder.create(itemStack);
    }
}
