package de.nova.lifehearts.manager;

import de.nova.lifehearts.LifeHeartsPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/** Parses sound settings once per reload and keeps the message hot path free of YAML lookups. */
public final class SoundManager {
    private static final long COOLDOWN_MILLIS = 80L;

    private final LifeHeartsPlugin plugin;
    private final Map<UUID, Long> lastSound = new HashMap<>();
    private Map<String, SoundSpec> sounds = Map.of();
    private boolean enabled;

    public SoundManager(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "sounds.yml");
        if (!file.isFile()) {
            plugin.saveResource("sounds.yml", false);
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        enabled = configuration.getBoolean("sounds.enabled", true);
        ConfigurationSection section = configuration.getConfigurationSection("sounds.messages");
        if (section == null) {
            sounds = Map.of();
            return;
        }
        Map<String, SoundSpec> parsed = new HashMap<>();
        for (String key : section.getKeys(false)) {
            String path = key + '.';
            if (!section.getBoolean(path + "enabled", false)) {
                continue;
            }
            String soundName = section.getString(path + "sound", "BLOCK_NOTE_BLOCK_PLING");
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
                float volume = clamp((float) section.getDouble(path + "volume", 0.45D), 0F, 4F);
                float pitch = clamp((float) section.getDouble(path + "pitch", 1D), 0.5F, 2F);
                parsed.put(key, new SoundSpec(sound, volume, pitch));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger()
                        .warning("Ignoring invalid sound for message " + key + ": " + soundName);
            }
        }
        sounds = Map.copyOf(parsed);
    }

    public void play(CommandSender sender, String messageKey) {
        if (!enabled || !(sender instanceof Player player)) {
            return;
        }
        SoundSpec sound = sounds.get(messageKey);
        if (sound == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long previous = lastSound.getOrDefault(player.getUniqueId(), 0L);
        if (now - previous < COOLDOWN_MILLIS) {
            return;
        }
        player.playSound(player.getLocation(), sound.sound(), sound.volume(), sound.pitch());
        lastSound.put(player.getUniqueId(), now);
    }

    public void unload(UUID playerId) {
        lastSound.remove(playerId);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record SoundSpec(Sound sound, float volume, float pitch) {}
}
