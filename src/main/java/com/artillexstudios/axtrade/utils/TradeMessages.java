package com.artillexstudios.axtrade.utils;

import com.artillexstudios.axapi.nms.wrapper.ServerPlayerWrapper;
import com.artillexstudios.axapi.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static com.artillexstudios.axtrade.AxTrade.CONFIG;
import static com.artillexstudios.axtrade.AxTrade.LANG;

/**
 * Recipient-aware messaging using the GUI's ordered placeholder pipeline.
 * Machine-readable config values (sounds, permissions, aliases) are not text.
 */
public final class TradeMessages {
    public static final TradeMessages MESSAGES = new TradeMessages();

    private TradeMessages() {}

    public void sendLang(CommandSender recipient, String path) {
        sendLang(recipient, path, Map.of());
    }

    public void sendLang(CommandSender recipient, String path, Map<String, String> replacements) {
        String message = LANG.getString(path);
        if (message == null) return;
        sendFormatted(recipient, CONFIG.getString("prefix", "") + message, replacements);
    }

    public void sendFormatted(CommandSender recipient, String message) {
        sendFormatted(recipient, message, Map.of());
    }

    /** Trade summaries deliberately do not gain the configured chat prefix. */
    public void sendFormatted(CommandSender recipient, String message, Map<String, String> replacements) {
        if (message == null) return;
        String resolved = resolve(recipient, message, replacements);
        if (recipient instanceof Player player) {
            ServerPlayerWrapper.wrap(player).message(StringUtils.format(resolved));
        } else {
            recipient.sendMessage(StringUtils.formatToString(resolved));
        }
    }

    public static String resolve(@Nullable CommandSender recipient, String message,
                                 Map<String, String> replacements) {
        Player player = recipient instanceof Player p ? p : null;
        return GuiPlaceholderResolver.resolve(player, message, replacements);
    }

    /** Use for interactive button labels and hover components without losing events. */
    public static Component component(@Nullable CommandSender recipient, String message,
                                      Map<String, String> replacements) {
        return StringUtils.format(resolve(recipient, message, replacements));
    }
}
