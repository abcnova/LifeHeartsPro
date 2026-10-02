package de.nova.lifehearts.listener;

import de.nova.lifehearts.LifeHeartsPlugin;
import de.nova.lifehearts.manager.LifeManager;
import de.nova.lifehearts.manager.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {
    private final LifeHeartsPlugin plugin;
    private final LifeManager lifeManager;
    private final TeamManager teamManager;

    public PlayerJoinListener(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
        this.lifeManager = plugin.getLifeManager();
        this.teamManager = plugin.getTeamManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.lifeManager.rememberName(player);
        int lives = this.lifeManager.getLives(player);
        this.teamManager.handleJoin(player);
        if (!this.plugin.getConfig().getBoolean("join-message.enabled", true)) {
            return;
        }
        String team = this.teamManager.getTeam(player);
        if (team == null) {
            team = this.plugin.getLocaleManager().get("team-none");
        }
        this.plugin
                .getLocaleManager()
                .send(player, "join-status", "%lives%", String.valueOf(lives), "%team%", team);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.lifeManager.unload(player.getUniqueId());
        this.plugin.getSoundManager().unload(player.getUniqueId());
        this.teamManager.handleQuit(player);
    }
}
