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
import java.util.LinkedHashMap;
import java.util.Set;

public class ItemBuilderUtil {

    @NotNull
    public static ItemBuilder newBuilder(@NotNull Section section, @Nullable Player player) {
        return newBuilder(section, Map.of(), player);
    }

    @NotNull
    public static ItemBuilder newBuilder(@NotNull Section section, Map<String, String> replacements, @Nullable Player player) {
        return newBuilder(section, null, replacements, player);
    }

    // Slot/action settings are not item metadata and must never reach ItemBuilder.
    private static final Set<String> SHARED_CONTROL_KEYS =
            Set.of("slot", "accept", "cancel", "use-confirm-countdown", "currency");

    /**
     * Build the configured item; a confirmation state's explicit name/lore
     * overrides any shared values declared at the parent control.
     * No default click text or generated lore is applied.
     */
    @NotNull
    public static ItemBuilder newBuilder(@NotNull Section section, @Nullable Section shared,
                                         Map<String, String> replacements, @Nullable Player player) {
        // Resolve text and metadata before AxAPI builds the stack. Numeric and
        // boolean YAML values retain their types throughout the merge.
        Map<String, Object> settings = new LinkedHashMap<>();
        if (shared != null) {
            settings.putAll(GuiPlaceholderResolver.resolveItemSection(player, shared, replacements));
            for (String key : SHARED_CONTROL_KEYS) settings.remove(key);
        }
        settings.putAll(GuiPlaceholderResolver.resolveItemSection(player, section, replacements));

        // Accept underscore spelling for the ItemBuilder's canonical key.
        if (settings.containsKey("item_model") && !settings.containsKey("item-model")) {
            settings.put("item-model", settings.get("item_model"));
        }

        // vitem is the preferred VoxelCore display source, but the existing
        // voxelcore: and material: voxel:... forms continue to work.
        String identifier = VoxelItemReference.select(settings);
        if (settings.get("material") instanceof String material && material.startsWith("voxel:")) {
            settings.remove("material");
        }
        settings.remove("vitem");
        settings.remove("voxelcore");
        if (identifier != null) {
            if (VoxelItemReference.isModelAsset(identifier)) {
                // A slash denotes a raw resource-pack item-model asset rather
                // than a VoxelCore registered ContentID (which disallows '/').
                // The pack must provide the corresponding assets/<ns>/items JSON.
                if (VoxelCoreItemProvider.isEnabled()) {
                    settings.putIfAbsent("material", "PAPER");
                    settings.put("item-model", VoxelItemReference.modelKey(identifier));
                } else if (!settings.containsKey("material") && !settings.containsKey("type")) {
                    settings.put("material", "BARRIER");
                }
            } else {
                ItemStack registered = VoxelCoreItemProvider.create(identifier);
                if (registered != null) {
                    return customizeRegisteredItem(registered, settings);
                }
                // Keep an explicitly configured vanilla fallback material.
                if (!settings.containsKey("material") && !settings.containsKey("type")) {
                    settings.put("material", "BARRIER");
                }
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
