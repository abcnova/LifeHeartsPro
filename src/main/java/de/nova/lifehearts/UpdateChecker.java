package de.nova.lifehearts;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Performs the optional Modrinth update check outside the server thread. */
public final class UpdateChecker {
    private static final String MODRINTH_PAGE = "https://modrinth.com/plugin/lifeheartspro";
    private static final String SPIGOT_PAGE =
            "https://www.spigotmc.org/resources/lifeheartspro.133466/";
    private static final String CURSEFORGE_PAGE =
            "https://www.curseforge.com/minecraft/bukkit-plugins/lifeheartspro";
    private static final String API_URL =
            "https://api.modrinth.com/v2/project/lifeheartspro/version"
                    + "?featured=true&include_changelog=false";
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DOWNLOAD_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private final LifeHeartsPlugin plugin;
    private final HttpClient client =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

    public UpdateChecker(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkAsync() {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", false)) {
            return;
        }
        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> {
                            try {
                                UpdateInfo update = fetchLatestVersion();
                                String current = plugin.getDescription().getVersion();
                                if (update != null
                                        && compareVersions(update.version(), current) > 0) {
                                    Bukkit.getScheduler()
                                            .runTask(plugin, () -> notifyOutdated(update, current));
                                }
                            } catch (Exception exception) {
                                if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                                    plugin.getLogger()
                                            .warning(
                                                    "Update check failed: "
                                                            + exception.getMessage());
                                }
                            }
                        });
    }

    private UpdateInfo fetchLatestVersion() throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(5))
                        .header(
                                "User-Agent",
                                "LifeHeartsPro/" + plugin.getDescription().getVersion())
                        .header("Accept", "application/json")
                        .GET()
                        .build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            return null;
        }
        Matcher versionMatcher = VERSION_PATTERN.matcher(response.body());
        if (!versionMatcher.find()) {
            return null;
        }
        String version = unescape(versionMatcher.group(1));
        Matcher nameMatcher = NAME_PATTERN.matcher(response.body());
        String name = nameMatcher.find() ? unescape(nameMatcher.group(1)) : version;
        Matcher downloadMatcher = DOWNLOAD_PATTERN.matcher(response.body());
        String download =
                downloadMatcher.find() ? unescape(downloadMatcher.group(1)) : MODRINTH_PAGE;
        return new UpdateInfo(name, version, download);
    }

    private void notifyOutdated(UpdateInfo update, String currentVersion) {
        String prefix = "[LifeHeartsPro] ";
        plugin.getLogger()
                .warning(
                        prefix
                                + "New version available: "
                                + update.name()
                                + " ("
                                + update.version()
                                + ')');
        plugin.getLogger().warning(prefix + "Current version: " + currentVersion);
        plugin.getLogger().warning(prefix + "Download: " + update.downloadUrl());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("lifeheartspro.admin")
                    && !player.hasPermission("lifeheartspro.update.notify")) {
                continue;
            }
            Component brandedPrefix =
                    Component.text(prefix, NamedTextColor.AQUA, TextDecoration.BOLD);
            player.sendMessage(
                    brandedPrefix
                            .append(
                                    Component.text(
                                            "New version available: ", NamedTextColor.YELLOW))
                            .append(
                                    Component.text(
                                            update.name() + " (" + update.version() + ')',
                                            NamedTextColor.WHITE)));
            player.sendMessage(
                    brandedPrefix
                            .append(Component.text("Current version: ", NamedTextColor.RED))
                            .append(Component.text(currentVersion, NamedTextColor.WHITE)));
            player.sendMessage(
                    brandedPrefix
                            .append(link("[Download]", update.downloadUrl(), NamedTextColor.GREEN))
                            .append(Component.space())
                            .append(link("[Modrinth]", MODRINTH_PAGE, NamedTextColor.AQUA))
                            .append(Component.space())
                            .append(link("[SpigotMC]", SPIGOT_PAGE, NamedTextColor.GOLD))
                            .append(Component.space())
                            .append(
                                    link(
                                            "[CurseForge]",
                                            CURSEFORGE_PAGE,
                                            NamedTextColor.LIGHT_PURPLE)));
            plugin.getSoundManager().play(player, "update_available");
        }
    }

    private Component link(String label, String url, NamedTextColor color) {
        return Component.text(label, color, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(url));
    }

    static int compareVersions(String left, String right) {
        int[] leftParts = versionParts(left);
        int[] rightParts = versionParts(right);
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            int leftPart = index < leftParts.length ? leftParts[index] : 0;
            int rightPart = index < rightParts.length ? rightParts[index] : 0;
            if (leftPart != rightPart) {
                return Integer.compare(leftPart, rightPart);
            }
        }
        return 0;
    }

    private static int[] versionParts(String version) {
        return Arrays.stream(version.split("[^0-9]+"))
                .filter(part -> !part.isBlank())
                .mapToInt(
                        part -> {
                            try {
                                return Integer.parseInt(part);
                            } catch (NumberFormatException ignored) {
                                return 0;
                            }
                        })
                .toArray();
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\/", "/");
    }

    private record UpdateInfo(String name, String version, String downloadUrl) {}
}
