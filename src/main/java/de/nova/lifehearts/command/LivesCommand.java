package de.nova.lifehearts.command;

import de.nova.lifehearts.LifeHeartsPlugin;
import de.nova.lifehearts.manager.LifeManager;
import de.nova.lifehearts.manager.LocaleManager;
import de.nova.lifehearts.manager.TeamManager;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class LivesCommand implements CommandExecutor, TabCompleter {
    private final LifeManager lifeManager;
    private final TeamManager teamManager;
    private final LocaleManager locale;

    public LivesCommand(LifeHeartsPlugin plugin) {
        this.lifeManager = plugin.getLifeManager();
        this.teamManager = plugin.getTeamManager();
        this.locale = plugin.getLocaleManager();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.locale.send(sender, "players-only");
            return true;
        }
        if (!this.has(player, "lifeheartspro.lives.view")) {
            this.locale.send(player, "no-permission");
            return true;
        }
        Player target = player;
        if (args.length >= 1) {
            if (!this.has(player, "lifeheartspro.lives.view.others")) {
                this.locale.send(player, "no-permission");
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                this.locale.send(player, "player-not-found");
                return true;
            }
        }
        int lives = this.lifeManager.getLives(target);
        String team = this.teamManager.getTeam(target);
        if (team == null) {
            team = this.locale.get("team-none");
        }
        player.sendMessage(this.locale.get("lives-status-title"));
        player.sendMessage(
                this.locale.replace(
                        this.locale.get("lives-status-player"), "%player%", target.getName()));
        player.sendMessage(
                this.locale.replace(
                        this.locale.get("lives-status-lives"), "%lives%", String.valueOf(lives)));
        player.sendMessage(
                this.locale.replace(this.locale.get("lives-status-team"), "%team%", team));
        this.locale.playSound(player, "lives-status-title");
        return true;
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("lifeheartspro.admin");
    }

    public List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !has(sender, "lifeheartspro.lives.view.others")) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
