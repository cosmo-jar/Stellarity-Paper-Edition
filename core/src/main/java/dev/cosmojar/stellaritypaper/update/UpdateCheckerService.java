package dev.cosmojar.stellaritypaper.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class UpdateCheckerService {

    public static final String PERMISSION = "stellarity.updates";

    private static final String GITHUB_REPO = "cosmo-jar/Stellarity-Paper-Edition";
    private static final String MODRINTH_SLUG = "stellarity-paper-edition";
    private static final String HANGAR_SLUG = "Stellarity-Paper-Edition";
    private static final String USER_AGENT_FORMAT = "cosmo-jar/Stellarity-Paper-Edition/%s (https://github.com/cosmo-jar/Stellarity-Paper-Edition)";

    private final StellarityPaperPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final AtomicReference<UpdateInfo> cachedUpdate = new AtomicReference<>();
    private final Set<UUID> notifiedPlayers = ConcurrentHashMap.newKeySet();
    private final HttpClient httpClient;

    public UpdateCheckerService(final StellarityPaperPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void start() {
        CompletableFuture.runAsync(this::runCheck);
    }

    public boolean hasUpdate() {
        return cachedUpdate.get() != null;
    }

    public void notifyPlayer(final Player player) {
        final UpdateInfo info = cachedUpdate.get();
        if (info == null) {
            return;
        }

        if (!notifiedPlayers.add(player.getUniqueId())) {
            return;
        }

        player.sendMessage(miniMessage.deserialize(
                "<gradient:#9333EA:#C084FC:#F472B6><bold>✦ Stellarity</bold></gradient> <gray>•</gray> <gradient:#E879F9:#F472B6>A new update is available: <bold>" + info.newVersion() + "</bold></gradient> <dark_gray>(installed: " + info.currentVersion() + ")</dark_gray>"
        ));
        player.sendMessage(miniMessage.deserialize(
                "<click:open_url:'" + info.downloadUrl() + "'><hover:show_text:'<gradient:#C084FC:#F472B6>Click to visit download page (" + info.sourceName() + ")</gradient>'><gradient:#818CF8:#C084FC><underlined>[Click here to download update]</underlined></gradient></hover></click>"
        ));
    }

    private void runCheck() {
        final String currentVersion = plugin.getPluginMeta().getVersion();

        try {
            // 1. Try GitHub Releases
            UpdateInfo info = checkGitHub(currentVersion);

            // 2. Fallback to Modrinth
            if (info == null) {
                info = checkModrinth(currentVersion);
            }

            // 3. Fallback to Hangar
            if (info == null) {
                info = checkHangar(currentVersion);
            }

            if (info != null) {
                if (isNewerVersion(info.newVersion(), currentVersion)) {
                    cachedUpdate.set(info);
                    logUpdateAvailable(info);
                    notifyOnlineAdmins();
                } else {
                    plugin.getLogger().fine("Update check completed: plugin is running the latest version (" + currentVersion + ").");
                }
            } else {
                plugin.getLogger().info("Update check completed: remote repositories are not reachable or plugin is not yet published.");
            }
        } catch (final Throwable throwable) {
            plugin.getLogger().info("Update check skipped: unable to connect to remote update servers (" + throwable.getMessage() + ").");
        }
    }

    private void logUpdateAvailable(final UpdateInfo info) {
        plugin.getComponentLogger().info(miniMessage.deserialize(
                "<gradient:#C084FC:#E879F9><bold>A new update is available: " + info.newVersion() + "</bold></gradient> <gray>(current: " + info.currentVersion() + ", source: " + info.sourceName() + ")</gray>"
        ));
        plugin.getComponentLogger().info(miniMessage.deserialize(
                "<gray>Download update: </gray><gradient:#818CF8:#C084FC><underlined>" + info.downloadUrl() + "</underlined></gradient>"
        ));
    }

    private void notifyOnlineAdmins() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(PERMISSION)) {
                    notifyPlayer(player);
                }
            }
        });
    }

    private UpdateInfo checkGitHub(final String currentVersion) {
        final String url = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", String.format(Locale.ROOT, USER_AGENT_FORMAT, currentVersion))
                    .header("Accept", "application/vnd.github.v3+json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                final JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("tag_name")) {
                    final String tag = json.get("tag_name").getAsString();
                    final String htmlUrl = json.has("html_url")
                            ? json.get("html_url").getAsString()
                            : "https://github.com/" + GITHUB_REPO + "/releases/latest";
                    return new UpdateInfo(tag, currentVersion, htmlUrl, "GitHub");
                }
            }
        } catch (final Exception ignored) {
        }
        return null;
    }

    private UpdateInfo checkModrinth(final String currentVersion) {
        final String url = "https://api.modrinth.com/v2/project/" + MODRINTH_SLUG + "/version";
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", String.format(Locale.ROOT, USER_AGENT_FORMAT, currentVersion))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                final JsonElement parsed = JsonParser.parseString(response.body());
                if (parsed.isJsonArray()) {
                    final JsonArray array = parsed.getAsJsonArray();
                    if (!array.isEmpty()) {
                        final JsonObject latest = array.get(0).getAsJsonObject();
                        if (latest.has("version_number")) {
                            final String ver = latest.get("version_number").getAsString();
                            final String projectUrl = "https://modrinth.com/plugin/" + MODRINTH_SLUG;
                            return new UpdateInfo(ver, currentVersion, projectUrl, "Modrinth");
                        }
                    }
                }
            }
        } catch (final Exception ignored) {
        }
        return null;
    }

    private UpdateInfo checkHangar(final String currentVersion) {
        final String url = "https://hangar.papermc.io/api/v1/projects/" + HANGAR_SLUG + "/versions?limit=1";
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", String.format(Locale.ROOT, USER_AGENT_FORMAT, currentVersion))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                final JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("result") && json.get("result").isJsonArray()) {
                    final JsonArray result = json.getAsJsonArray("result");
                    if (!result.isEmpty()) {
                        final JsonObject latest = result.get(0).getAsJsonObject();
                        if (latest.has("name")) {
                            final String ver = latest.get("name").getAsString();
                            final String projectUrl = "https://hangar.papermc.io/cosmo-jar/" + HANGAR_SLUG;
                            return new UpdateInfo(ver, currentVersion, projectUrl, "Hangar");
                        }
                    }
                }
            }
        } catch (final Exception ignored) {
        }
        return null;
    }

    public static boolean isNewerVersion(final String remote, final String current) {
        if (remote == null || current == null || remote.isBlank() || current.isBlank()) {
            return false;
        }

        final String cleanRemote = cleanVersion(remote);
        final String cleanCurrent = cleanVersion(current);

        if (cleanRemote.equals(cleanCurrent)) {
            final boolean currentIsPre = isPreRelease(current);
            final boolean remoteIsPre = isPreRelease(remote);
            return currentIsPre && !remoteIsPre;
        }

        final int[] remoteParts = parseVersionNumbers(cleanRemote);
        final int[] currentParts = parseVersionNumbers(cleanCurrent);
        final int maxLength = Math.max(remoteParts.length, currentParts.length);

        for (int i = 0; i < maxLength; i++) {
            final int r = i < remoteParts.length ? remoteParts[i] : 0;
            final int c = i < currentParts.length ? currentParts[i] : 0;
            if (r > c) {
                return true;
            }
            if (r < c) {
                return false;
            }
        }

        return false;
    }

    private static String cleanVersion(final String v) {
        String s = v.trim();
        if (s.startsWith("v") || s.startsWith("V")) {
            s = s.substring(1);
        }
        final int dashIdx = s.indexOf('-');
        if (dashIdx != -1) {
            s = s.substring(0, dashIdx);
        }
        final int plusIdx = s.indexOf('+');
        if (plusIdx != -1) {
            s = s.substring(0, plusIdx);
        }
        return s;
    }

    private static boolean isPreRelease(final String v) {
        final String lower = v.toLowerCase(Locale.ROOT);
        return lower.contains("-snapshot") || lower.contains("-beta") || lower.contains("-alpha") || lower.contains("-rc");
    }

    private static int[] parseVersionNumbers(final String clean) {
        final String[] tokens = clean.split("\\.");
        final int[] result = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            try {
                result[i] = Integer.parseInt(tokens[i].replaceAll("[^0-9]", ""));
            } catch (final NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }
}
