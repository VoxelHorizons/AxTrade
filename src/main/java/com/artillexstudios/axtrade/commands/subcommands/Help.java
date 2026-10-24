package com.artillexstudios.axtrade.commands.subcommands;

import org.bukkit.command.CommandSender;

import static com.artillexstudios.axtrade.AxTrade.LANG;
import static com.artillexstudios.axtrade.utils.TradeMessages.MESSAGES;

public enum Help {
    INSTANCE;

    public void execute(CommandSender sender) {
        if (sender.hasPermission("axtrade.admin")) {
            for (String m : LANG.getStringList("admin-help")) {
                MESSAGES.sendFormatted(sender, m);
            }
        } else {
            for (String m : LANG.getStringList("player-help")) {
                MESSAGES.sendFormatted(sender, m);
            }
        }
    }
}
