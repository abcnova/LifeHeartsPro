package de.nova.lifehearts.manager;

import de.nova.lifehearts.LifeHeartsPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** Maintains the bounded in-memory life cache and persists changes through {@link DataManager}. */
public final class LifeManager {
    private final LifeHeartsPlugin plugin;
    private final Map<UUID, Integer> lives = new HashMap<>();

    public LifeManager(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
    }

    public int getLives(Player player) {
        return getLives(player.getUniqueId());
    }

    public int getLives(OfflinePlayer player) {
        return getLives(player.getUniqueId());
    }

    public int getLives(UUID uuid) {
        return lives.computeIfAbsent(
                uuid,
                key -> {
                    int fallback = plugin.getConfig().getInt("lives.default", 3);
                    int stored =
                            plugin.getDataManager()
                                    .getConfig()
                                    .getInt("players." + key + ".lives", fallback);
                    return clampLives(stored);
                });
    }

    public void setLives(Player player, int amount) {
        rememberName(player);
        setLives(player.getUniqueId(), amount);
    }

    public void setLives(OfflinePlayer player, int amount) {
        if (player.getName() != null) {
            rememberName(player.getUniqueId(), player.getName());
        }
        setLives(player.getUniqueId(), amount);
    }

    public void setLives(UUID uuid, int amount) {
        setLivesInMemory(uuid, amount);
        plugin.getDataManager().save();
    }

    /** Applies a transfer as one validated in-memory transaction and one persistent snapshot. */
    public boolean transferLives(Player sender, Player recipient, int amount, int minimumAfter) {
        if (sender.getUniqueId().equals(recipient.getUniqueId()) || amount <= 0) {
            return false;
        }
        int senderLives = getLives(sender);
        int recipientLives = getLives(recipient);
        if (senderLives - amount < minimumAfter || recipientLives + amount > getMaximumLives()) {
            return false;
        }
        rememberName(sender);
        rememberName(recipient);
        setLivesInMemory(sender.getUniqueId(), senderLives - amount);
        setLivesInMemory(recipient.getUniqueId(), recipientLives + amount);
        plugin.getDataManager().save();
        return true;
    }

    private void setLivesInMemory(UUID uuid, int amount) {
        int clamped = clampLives(amount);
        lives.put(uuid, clamped);
        plugin.getDataManager().getConfig().set("players." + uuid + ".lives", clamped);
    }

    public int clampLives(int amount) {
        int min = plugin.getConfig().getInt("lives.min", 0);
        int max = Math.max(min, plugin.getConfig().getInt("lives.max", 20));
        return Math.max(min, Math.min(max, amount));
    }

    public int getMaximumLives() {
        return Math.max(
                plugin.getConfig().getInt("lives.min", 0),
                plugin.getConfig().getInt("lives.max", 20));
    }

    public void clampAllOnlineLivesToBounds() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            setLivesInMemory(player.getUniqueId(), getLives(player));
        }
        plugin.getDataManager().save();
    }

    public void resetAllOnlineLivesToDefault() {
        int defaultLives = plugin.getConfig().getInt("lives.default", 3);
        for (Player player : Bukkit.getOnlinePlayers()) {
            setLivesInMemory(player.getUniqueId(), defaultLives);
        }
        plugin.getDataManager().save();
    }

    public void addLives(Player player, int amount) {
        setLives(player, getLives(player) + amount);
    }

    public void removeLives(Player player, int amount) {
        setLives(player, getLives(player) - amount);
    }

    public boolean isDead(Player player) {
        return getLives(player) <= 0;
    }

    public boolean isDead(OfflinePlayer player) {
        return getLives(player) <= 0;
    }

    public void revivePlayer(Player player) {
        revivePlayer((OfflinePlayer) player);
    }

    public void revivePlayer(OfflinePlayer player) {
        setLives(player, plugin.getConfig().getInt("lives.default", 3));
        String name = player.getName();
        if (name != null) {
            Bukkit.getBanList(BanList.Type.NAME).pardon(name);
        }
    }

    public void handleDeath(Player player) {
        if (plugin.getConfig().getBoolean("lives.lose-life-on-death", true)) {
            removeLives(player, 1);
        }
    }

    public void handleElimination(Player player) {
        if (!isDead(player)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("lives.ban-on-zero-lives", true)) {
            plugin.getLocaleManager().send(player, "eliminated");
            return;
        }
        String message = plugin.getLocaleManager().get("ban-message");
        Bukkit.getBanList(BanList.Type.NAME)
                .addBan(player.getName(), message, null, "LifeHeartsPro");
        player.kickPlayer(message);
    }

    public void rememberName(Player player) {
        rememberName(player.getUniqueId(), player.getName());
    }

    private void rememberName(UUID uuid, String name) {
        String path = "players." + uuid + ".last-known-name";
        if (!name.equals(plugin.getDataManager().getConfig().getString(path))) {
            plugin.getDataManager().getConfig().set(path, name);
            plugin.getDataManager().save();
        }
    }

    public void unload(UUID uuid) {
        lives.remove(uuid);
    }

    public void clearCache() {
        lives.clear();
    }
}
