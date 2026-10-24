package com.artillexstudios.axtrade.request;

import com.artillexstudios.axapi.nms.wrapper.ServerPlayerWrapper;
import com.artillexstudios.axtrade.api.events.AxTradeRequestEvent;
import com.artillexstudios.axtrade.safety.SafetyManager;
import com.artillexstudios.axtrade.trade.Trades;
import com.artillexstudios.axtrade.utils.SoundUtils;
import com.artillexstudios.axtrade.utils.TradeMessages;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.artillexstudios.axtrade.AxTrade.CONFIG;
import static com.artillexstudios.axtrade.AxTrade.LANG;
import static com.artillexstudios.axtrade.utils.TradeMessages.MESSAGES;
import static com.artillexstudios.axtrade.AxTrade.TOGGLED;

public class Requests {
    private static final List<Request> requests = new ArrayList<>();

    public static void addRequest(@NotNull Player sender, @NotNull Player receiver) {
        if (!SafetyManager.TRADING.get()) {
            MESSAGES.sendLang(sender, "safety");
            return;
        }

        Map<String, String> replacements = Map.of("%player%", receiver.getName());

        boolean disallowSameIp = CONFIG.getBoolean("disallow-same-ip-trade", false);
        if (disallowSameIp && sender.getAddress() != null && receiver.getAddress() != null && sender.getAddress().getAddress().equals(receiver.getAddress().getAddress())) {
            MESSAGES.sendLang(sender, "request.same-ip", replacements);
            return;
        }

        var disallowed = CONFIG.getStringList("disallowed-gamemodes");
        if (disallowed.contains(sender.getGameMode().name()) || disallowed.contains(receiver.getGameMode().name())) {
            MESSAGES.sendLang(sender, "request.disallowed-gamemode", replacements);
            return;
        }

        var blacklisted = CONFIG.getStringList("blacklisted-worlds");
        if (blacklisted.contains(sender.getWorld().getName()) || blacklisted.contains(receiver.getWorld().getName())) {
            MESSAGES.sendLang(sender, "request.blacklisted-world", replacements);
            return;
        }

        if (sender.isDead() || receiver.isDead()) {
            MESSAGES.sendLang(sender, "request.not-accepting", replacements);
            return;
        }

        if (TOGGLED.getBoolean("toggled." + receiver.getUniqueId())) {
            MESSAGES.sendLang(sender, "request.not-accepting", replacements);
            return;
        }

        if (TOGGLED.getBoolean("toggled." + sender.getUniqueId())) {
            MESSAGES.sendLang(sender, "request.disabled-trading", replacements);
            return;
        }

        if (sender.equals(receiver)) {
            MESSAGES.sendLang(sender, "request.cant-trade-self", replacements);
            return;
        }

        if (Trades.isTrading(receiver) || Trades.isTrading(sender)) {
            MESSAGES.sendLang(sender, "request.already-in-trade", replacements);
            return;
        }

        int maxDist = CONFIG.getInt("trade-max-distance", 10);
        if (maxDist != -1 && (sender.getWorld() != receiver.getWorld() || maxDist < sender.getLocation().distance(receiver.getLocation()))) {
            MESSAGES.sendLang(sender, "request.too-far", replacements);
            return;
        }

        Request request = Requests.getRequest(sender, receiver);
        if (request != null) {
            if (request.isActive() && !request.getSender().equals(sender)) {
                Trades.addTrade(sender, receiver);
                requests.remove(request);
                request.deactivate();
                return;
            }
            if (!request.isActive() || System.currentTimeMillis() - request.getTime() < CONFIG.getInt("trade-request-expire-seconds", 60) * 1_000L) {
                MESSAGES.sendLang(sender, "request.already-sent", replacements);
                return;
            }
        }

        AxTradeRequestEvent apiEvent = new AxTradeRequestEvent(sender, receiver);
        Bukkit.getPluginManager().callEvent(apiEvent);
        if (apiEvent.isCancelled()) return;

        requests.add(new Request(sender, receiver));

        MESSAGES.sendLang(sender, "request.sent-sender", replacements);

        Map<String, String> replacements2 = Map.of("%player%", sender.getName());
        if (LANG.getSection("request.sent-receiver") == null) // this is for backwards compatibility
            MESSAGES.sendLang(receiver, "request.sent-receiver", replacements2);
        else {
            ServerPlayerWrapper receiverWrap = ServerPlayerWrapper.wrap(receiver);
            receiverWrap.message(TradeMessages.component(receiver,
                    CONFIG.getString("prefix", "") + LANG.getString("request.sent-receiver.info"), replacements2));
            receiverWrap.message(TradeMessages.component(receiver, LANG.getString("request.sent-receiver.accept.message"), replacements2)
                    .hoverEvent(HoverEvent.showText(TradeMessages.component(receiver, LANG.getString("request.sent-receiver.accept.hover"), replacements2)))
                    .clickEvent(ClickEvent.runCommand("/trade accept " + sender.getName())));
            receiverWrap.message(TradeMessages.component(receiver, LANG.getString("request.sent-receiver.deny.message"), replacements2)
                    .hoverEvent(HoverEvent.showText(TradeMessages.component(receiver, LANG.getString("request.sent-receiver.deny.hover"), replacements2)))
                    .clickEvent(ClickEvent.runCommand("/trade deny " + sender.getName())));
        }
        SoundUtils.playSound(sender, "requested");
        SoundUtils.playSound(receiver, "requested");
    }

    @Nullable
    public static Request getRequest(Player p1, Player p2) {
        for (Request rq : requests) {
            if (p1.equals(rq.getSender()) && p2.equals(rq.getReceiver())) return rq;
            if (p2.equals(rq.getSender()) && p1.equals(rq.getReceiver())) return rq;
        }
        return null;
    }

    public static boolean hasRequest(Player p1, Player p2) {
        return getRequest(p1, p2) != null;
    }

    public static List<Request> getRequests() {
        return requests;
    }
}