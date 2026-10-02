package de.nova.lifehearts.command;

import de.nova.lifehearts.LifeHeartsPlugin;
import de.nova.lifehearts.manager.LocaleManager;
import de.nova.lifehearts.manager.TeamManager;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class TeamCommand implements CommandExecutor, TabCompleter {
    private final LifeHeartsPlugin plugin;
    private final TeamManager teamManager;
    private final LocaleManager locale;

    public TeamCommand(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.locale = plugin.getLocaleManager();
    }

    private void sendHelp(Player player) {
        this.locale.send(player, "team-usage");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.locale.send(sender, "players-only");
            return true;
        }
        if (args.length == 0) {
            this.sendHelp(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create":
                {
                    if (!this.has(player, "lifeheartspro.team.create")
                            || !this.plugin
                                    .getConfig()
                                    .getBoolean("teams.allow-player-teams", true)) {
                        this.locale.send(player, "no-permission");
                        return true;
                    }
                    if (args.length < 2) {
                        this.locale.send(player, "team-usage");
                        return true;
                    }
                    if (this.teamManager.createTeam(player, args[1])) {
                        this.locale.send(player, "team-created", "%team%", args[1]);
                        break;
                    }
                    this.locale.send(player, "team-create-failed");
                    break;
                }
            case "invite":
                {
                    if (!this.has(player, "lifeheartspro.team.invite")) {
                        this.locale.send(player, "no-permission");
                        return true;
                    }
                    if (args.length < 2) {
                        this.locale.send(player, "team-usage");
                        return true;
                    }
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        this.locale.send(player, "player-not-found");
                        return true;
                    }
                    if (this.teamManager.invite(player, target)) {
                        this.locale.send(player, "team-invite-sent", "%player%", target.getName());
                        this.locale.send(
                                target, "team-invite-received", "%player%", player.getName());
                        break;
                    }
                    this.locale.send(player, "team-invite-failed");
                    break;
                }
            case "accept":
                {
                    if (!this.has(player, "lifeheartspro.team.accept")) {
                        this.locale.send(player, "no-permission");
                        return true;
                    }
                    if (this.teamManager.acceptInvite(player)) {
                        this.locale.send(player, "team-joined");
                        break;
                    }
                    this.locale.send(player, "team-accept-failed");
                    break;
                }
            case "kick":
                {
                    if (!this.has(player, "lifeheartspro.team.kick")
                            || !this.plugin.getConfig().getBoolean("teams.allow-kick", true)) {
                        this.locale.send(player, "no-permission");
                        return true;
                    }
                    if (args.length < 2) {
                        this.locale.send(player, "team-usage");
                        return true;
                    }
                    Player kickTarget = Bukkit.getPlayerExact(args[1]);
                    if (kickTarget == null) {
                        this.locale.send(player, "player-not-found");
                        return true;
                    }
                    if (this.teamManager.kick(player, kickTarget)) {
                        this.locale.send(player, "team-kicked", "%player%", kickTarget.getName());
                        this.locale.send(kickTarget, "team-you-were-kicked");
                        break;
                    }
                    this.locale.send(player, "team-kick-failed");
                    break;
                }
            case "leave":
                {
                    if (!this.has(player, "lifeheartspro.team.leave")
                            || !this.plugin.getConfig().getBoolean("teams.allow-leave", true)) {
                        this.locale.send(player, "no-permission");
                        return true;
                    }
                    if (this.teamManager.getTeam(player) == null) {
                        this.locale.send(player, "team-not-in-team");
                        return true;
                    }
                    this.teamManager.leaveTeam(player);
                    this.locale.send(player, "team-left");
                    break;
                }
            case "disband":
                {
                    if (!this.has(player, "lifeheartspro.team.disband")
                            || !this.plugin.getConfig().getBoolean("teams.allow-disband", true)) {
                        this.locale.send(player, "no-permission");
                        return true;
                    }
                    if (this.teamManager.disband(player)) {
                        this.locale.send(player, "team-disbanded");
                        break;
                    }
                    this.locale.send(player, "team-disband-failed");
                    break;
                }
            case "info":
                {
                    if (!this.has(player, "lifeheartspro.team.info")) {
                        this.locale.send(player, "no-permission");
                        return true;
                    }
                    String team = this.teamManager.getTeam(player);
                    if (team == null) {
                        this.locale.send(player, "team-not-in-team");
                        return true;
                    }
                    Set<String> members = this.teamManager.getTeamMembers(team);
                    this.locale.send(player, "team-info-title");
                    player.sendMessage(
                            this.locale.replace(this.locale.get("team-info-name"), "%team%", team));
                    player.sendMessage(
                            this.locale.replace(
                                    this.locale.get("team-info-members"),
                                    "%amount%",
                                    String.valueOf(members.size())));
                    this.locale.playSound(player, "team-info-title");
                    for (String member : members) {
                        player.sendMessage(" \u00a78- \u00a7f" + member);
                    }
                    break;
                }
            case "list":
                {
                    if (!this.has(player, "lifeheartspro.team.list")) {
                        this.locale.send(player, "no-permission");
                        return true;
                    }
                    this.locale.send(player, "team-list-title");
                    for (String teamName : this.teamManager.getTeams()) {
                        int size = this.teamManager.getTeamMembers(teamName).size();
                        player.sendMessage(
                                this.locale.replace(
                                        this.locale.get("team-list-entry"),
                                        "%team%",
                                        teamName,
                                        "%amount%",
                                        String.valueOf(size)));
                    }
                    this.locale.playSound(player, "team-list-title");
                    break;
                }
            default:
                {
                    this.sendHelp(player);
                }
        }
        return true;
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("lifeheartspro.admin");
    }

    public List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args) {
        List<String> candidates = List.of();
        if (args.length == 1) {
            candidates =
                    Arrays.asList(
                            "create", "invite", "accept", "kick", "leave", "disband", "info",
                            "list");
        }
        if (args.length == 2
                && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            candidates = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
