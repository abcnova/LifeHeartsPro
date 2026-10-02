package de.nova.lifehearts.listener;

import de.nova.lifehearts.LifeHeartsPlugin;
import de.nova.lifehearts.manager.TeamManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class TeamRespawnListener implements Listener {
    private final LifeHeartsPlugin plugin;
    private final TeamManager teamManager;

    public TeamRespawnListener(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn() || event.isAnchorSpawn()) {
            return;
        }
        Player player = event.getPlayer();
        String team = this.teamManager.getTeam(player);
        Location teamSpawn = this.teamManager.getTeamSpawn(team);
        if (teamSpawn != null) {
            event.setRespawnLocation(teamSpawn);
        }
        this.plugin
                .getServer()
                .getScheduler()
                .runTask(this.plugin, () -> this.teamManager.applyGlowing(player));
    }
}
