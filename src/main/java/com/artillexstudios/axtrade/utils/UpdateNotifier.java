package com.artillexstudios.axtrade.utils;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axtrade.AxTrade;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

public class UpdateNotifier implements Listener {
    private static Config config;
    private static Config lang;
    private static boolean onJoin;
    private static String prefix;
    private static String updateNotifier;

    private final String current;
    private String latest = null;
    private boolean newest = true;

    public static void init(Config config, Config lang) {
        UpdateNotifier.config = config;
        UpdateNotifier.lang = lang;
        reload();
    }

    public static void reload() {
        onJoin = config.getBoolean("update-notifier.on-join", true);
        prefix = config.getString("prefix");
        updateNotifier = lang.getString("update-notifier");
    }

    public UpdateNotifier() {
        this.current = AxTrade.getInstance().getDescription().getVersion();

        AxTrade.getInstance().getServer().getPluginManager().registerEvents(this, AxTrade.getInstance());

        long time = 30L * 60L * 20L;
        Scheduler.get().runAsyncTimer(t -> {
            this.latest = readVersion();
            this.newest = !isOutdated(current);

            if (latest == null || newest) return;
            Scheduler.get().runLater(t2 -> sendUpdate(Bukkit.getConsoleSender()), 50L);
            t.cancel();
        }, 0, time);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (latest == null || newest) return;
        if (!onJoin) return;
        if (!event.getPlayer().hasPermission(AxTrade.getInstance().getName().toLowerCase() + ".update-notify")) return;
        Player player = event.getPlayer();
        // Evaluate player-specific PAPI expansions on the player's region thread.
        Scheduler.get().runLaterAsync(t ->
                Scheduler.get().run(player, task -> sendUpdate(player), () -> {}), 50L);
    }

    private void sendUpdate(CommandSender recipient) {
        TradeMessages.MESSAGES.sendFormatted(recipient, String.format("%s %s", prefix, updateNotifier),
                Map.of("%current%", current, "%latest%", latest));
    }

    @Nullable
    private String readVersion() {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://www.artillex-studios.com/api/v1/resource/%s/latest-version".formatted(AxTrade.getInstance().getName())))
                    .timeout(Duration.of(10, SECONDS))
                    .GET()
                    .build();

            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            return response.body().toString();
        } catch (Exception ex) {
            return null;
        }
    }

    public String getLatest() {
        return latest;
    }

    public boolean isOutdated(String current) {
        // Fork/test suffixes such as 1.28.2-voxel-gui-test must not crash
        // the asynchronous update task. Malformed HTTP replies are ignored.
        return VersionComparison.isNewer(latest, current);
    }
}