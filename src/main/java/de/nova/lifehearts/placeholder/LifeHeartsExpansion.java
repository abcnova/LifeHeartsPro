package de.nova.lifehearts.placeholder;

import de.nova.lifehearts.LifeHeartsPlugin;
import de.nova.lifehearts.manager.LifeManager;
import de.nova.lifehearts.manager.TeamManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class LifeHeartsExpansion extends PlaceholderExpansion {
    private final LifeHeartsPlugin plugin;
    private final LifeManager lifeManager;
    private final TeamManager teamManager;

    public LifeHeartsExpansion(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
        this.lifeManager = plugin.getLifeManager();
        this.teamManager = plugin.getTeamManager();
    }

    public String getIdentifier() {
        return "lifehearts";
    }

    public String getAuthor() {
        return "ABC_Nova";
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        if (identifier.equals("lives")) {
            return String.valueOf(this.lifeManager.getLives(player));
        }
        if (identifier.equals("team")) {
            String team = this.teamManager.getTeam(player);
            return team == null ? "None" : team;
        }
        if (identifier.equals("team_size")) {
            String team = this.teamManager.getTeam(player);
            if (team == null) {
                return "0";
            }
            return String.valueOf(this.teamManager.getTeamMembers(team).size());
        }
        return null;
    }
}
