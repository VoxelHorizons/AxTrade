#!/usr/bin/env python3
"""Prepare a CI-only GUI test distribution from the unmodified full-source fork.

The upstream POM references private/non-redistributable system-scoped currency
libraries missing from GitHub. Instead of faking APIs or silently claiming a
full production build, prune those *optional* hooks in the CI workspace only.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
pom = root / "pom.xml"
ET.register_namespace("", "http://maven.apache.org/POM/4.0.0")
tree = ET.parse(pom)
ns = "{http://maven.apache.org/POM/4.0.0}"
deps = tree.getroot().find(ns + "dependencies")
assert deps is not None

# Keep only the dependency graph for AxTrade core trading and GUI rendering.
# Vault, experience and PAPI-backed currency remain supported in test builds.
keep = {
    ("org.spigotmc", "spigot-api"),
    ("com.artillexstudios.axapi", "axapi"),
    ("dev.triumphteam", "triumph-gui"),
    ("me.clip", "placeholderapi"),
    ("org.bstats", "bstats-bukkit"),
    ("com.github.MilkBowl", "VaultAPI"),
}
for dependency in list(deps):
    key = (dependency.findtext(ns + "groupId"), dependency.findtext(ns + "artifactId"))
    if key not in keep:
        deps.remove(dependency)

# Upstream uses Paper's bundled Adventure API without declaring it. This
# test build compiles against a provided API; the server supplies it at runtime.
adventure = ET.SubElement(deps, ns + "dependency")
ET.SubElement(adventure, ns + "groupId").text = "net.kyori"
ET.SubElement(adventure, ns + "artifactId").text = "adventure-api"
ET.SubElement(adventure, ns + "version").text = "4.17.0"
ET.SubElement(adventure, ns + "scope").text = "provided"

# Intentionally mark these as **GUI testing only**, not production builds.
version = tree.getroot().find(ns + "version")
assert version is not None
version.text = version.text + "-voxel-gui-test"

pom.write_bytes(ET.tostring(tree.getroot(), encoding="utf-8", xml_declaration=True))

currency = root / "src/main/java/com/artillexstudios/axtrade/hooks/currency"
allowed_currency = {"CurrencyHook.java", "ExperienceHook.java", "VaultHook.java", "PlaceholderCurrencyHook.java"}
for java in currency.glob("*.java"):
    if java.name not in allowed_currency:
        java.unlink()

hooks = root / "src/main/java/com/artillexstudios/axtrade/hooks/HookManager.java"
hooks.write_text('''package com.artillexstudios.axtrade.hooks;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axtrade.hooks.currency.CurrencyHook;
import com.artillexstudios.axtrade.hooks.currency.ExperienceHook;
import com.artillexstudios.axtrade.hooks.currency.VaultHook;
import com.artillexstudios.axtrade.hooks.currency.PlaceholderCurrencyHook;
import com.artillexstudios.axtrade.hooks.other.AxShulkersHook;
import com.artillexstudios.axtrade.hooks.other.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Map;
import static com.artillexstudios.axtrade.AxTrade.HOOKS;

/** GUI-test build only; other currency hooks require upstream private API JARs. */
public final class HookManager {
    private static final ArrayList<CurrencyHook> currency = new ArrayList<>();

    public static void setupHooks() {
        updateHooks();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholders();
        }
    }

    public static void updateHooks() {
        currency.removeIf(hook -> !hook.isPersistent());
        if (HOOKS.getBoolean("currencies.Experience.register", true)) {
            currency.add(new ExperienceHook());
        }
        if (HOOKS.getBoolean("currencies.Vault.register", true)
                && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            currency.add(new VaultHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString(
                "&#33FF33[AxTrade] Hooked into Vault!"));
        }
        Section section = HOOKS.getSection("placeholder-currencies");
        if (section != null) {
            for (String key : section.getRoutesAsStrings(false)) {
                if (!HOOKS.getBoolean("placeholder-currencies." + key + ".register", false)) continue;
                currency.add(new PlaceholderCurrencyHook(key,
                        HOOKS.getSection("placeholder-currencies." + key)));
            }
        }
        for (CurrencyHook hook : currency) hook.setup();
    }

    public static void registerCurrencyHook(@NotNull Plugin plugin,
                                             @NotNull CurrencyHook hook) {
        currency.add(hook);
        Section section = HOOKS.getSection("currencies." + hook.getName());
        if (section == null) {
            section = HOOKS.getBackingDocument().createSection("currencies." + hook.getName());
            section.set("enabled", true);
            section.set("name", hook.getName());
            section.set("tax", 0);
            HOOKS.save();
        }
    }

    @NotNull
    public static ArrayList<CurrencyHook> getCurrency() { return currency; }

    @Nullable
    public static CurrencyHook getCurrencyHook(@NotNull String name) {
        for (CurrencyHook hook : currency) {
            if (hook.getName().equals(name)) return hook;
        }
        return null;
    }

    @Nullable
    public static AxShulkersHook getAxShulkersHook() { return null; }
}
''', encoding="utf-8")

# Keep the no-op class as a compile-time type for existing shulker inspection.
# Vanilla shulker and bundle handling remains unchanged.
shulker = root / "src/main/java/com/artillexstudios/axtrade/hooks/other/AxShulkersHook.java"
shulker.write_text('''package com.artillexstudios.axtrade.hooks.other;

import org.bukkit.inventory.ItemStack;

/** Optional AxShulkers bridge omitted in GUI-test builds only. */
public final class AxShulkersHook {
    public ItemStack[] getItems(ItemStack item) { return null; }
}
''', encoding="utf-8")

print("Prepared GUI-test sources (core trades, Vault, experience, placeholder currency).")
