package de.nova.lifehearts.manager;

import de.nova.lifehearts.LifeHeartsPlugin;
import java.io.File;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

/** Loads one configured server language and provides consistent placeholder replacement. */
public final class LocaleManager {
    private static final String DEFAULT_LOCALE = "en_us";
    private static final String DEFAULT_PREFIX = "&b&lLifeHeartsPro &8» ";

    private final LifeHeartsPlugin plugin;
    private File file;
    private YamlConfiguration messages;
    private String prefix;

    public LocaleManager(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String locale = normalizeLocale(plugin.getConfig().getString("locale", DEFAULT_LOCALE));
        file = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        if (!file.isFile()) {
            plugin.getLogger()
                    .warning("Language file " + locale + ".yml was not found; using en_us.yml.");
            file = new File(plugin.getDataFolder(), "lang/en_us.yml");
            if (!file.isFile()) {
                plugin.saveResource("lang/en_us.yml", false);
            }
        }
        messages = YamlConfiguration.loadConfiguration(file);
        prefix = color(messages.getString("prefix", DEFAULT_PREFIX));
    }

    public String get(String key) {
        if ("prefix".equalsIgnoreCase(key)) {
            return prefix;
        }
        return color(findMessage(key));
    }

    public String getRaw(String key) {
        return findMessage(key);
    }

    public String getPrefixed(String key) {
        String message = get(key);
        return message.startsWith(prefix) ? message : prefix + message;
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(getPrefixed(key));
        plugin.getSoundManager().play(sender, key);
    }

    public void send(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(replace(getPrefixed(key), replacements));
        plugin.getSoundManager().play(sender, key);
    }

    public void playSound(CommandSender sender, String key) {
        plugin.getSoundManager().play(sender, key);
    }

    public String replace(String message, String... replacements) {
        if (replacements.length % 2 != 0) {
            plugin.getLogger().warning("Ignoring an unmatched message placeholder replacement.");
            return message;
        }
        String result = message;
        for (int index = 0; index < replacements.length; index += 2) {
            result = result.replace(replacements[index], replacements[index + 1]);
        }
        return color(result);
    }

    private String findMessage(String key) {
        String message = messages.getString(key);
        if (message != null) {
            return message;
        }
        plugin.getLogger().warning("Missing language key " + key + " in " + file.getName());
        return "Missing message: " + key;
    }

    private String color(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }

    private String normalizeLocale(String input) {
        if (input == null || input.isBlank()) {
            return DEFAULT_LOCALE;
        }
        String normalized = input.toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "en", "en_en", DEFAULT_LOCALE -> DEFAULT_LOCALE;
            case "de", "de_de", "de_eu" -> "de_eu";
            case "es", "es_es" -> "es_es";
            case "fr", "fr_fr" -> "fr_fr";
            default -> normalized;
        };
    }
}
