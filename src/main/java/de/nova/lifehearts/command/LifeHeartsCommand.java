package de.nova.lifehearts.command;

import de.nova.lifehearts.LifeHeartsPlugin;
import de.nova.lifehearts.manager.LifeManager;
import de.nova.lifehearts.manager.LocaleManager;
import de.nova.lifehearts.manager.TeamManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class LifeHeartsCommand implements CommandExecutor, TabCompleter {
    private final LifeHeartsPlugin plugin;
    private final LifeManager lifeManager;
    private final TeamManager teamManager;
    private final LocaleManager locale;

    public LifeHeartsCommand(LifeHeartsPlugin lifeHeartsPlugin) {
        this.plugin = lifeHeartsPlugin;
        this.lifeManager = lifeHeartsPlugin.getLifeManager();
        this.teamManager = lifeHeartsPlugin.getTeamManager();
        this.locale = lifeHeartsPlugin.getLocaleManager();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(this.locale.getPrefixed("help-title"));
        sender.sendMessage(this.locale.getPrefixed("help-reload"));
        sender.sendMessage(this.locale.getPrefixed("help-revive"));
        sender.sendMessage(this.locale.getPrefixed("help-lives-set"));
        sender.sendMessage(this.locale.getPrefixed("help-lives-add"));
        sender.sendMessage(this.locale.getPrefixed("help-lives-remove"));
        sender.sendMessage(this.locale.getPrefixed("help-actionbar"));
        sender.sendMessage(this.locale.getPrefixed("help-heartmode"));
        sender.sendMessage(this.locale.getPrefixed("help-team-glowing"));
        sender.sendMessage(this.locale.getPrefixed("help-team-tp"));
        sender.sendMessage(this.locale.getPrefixed("help-team-setspawn"));
        sender.sendMessage(this.locale.getPrefixed("help-team-admin"));
        this.locale.playSound(sender, "help-title");
    }

    public boolean onCommand(CommandSender sender, Command command, String string, String[] args) {
        if (command.getName().equalsIgnoreCase("revive")) {
            if (!this.has(sender, "lifeheartspro.revive")) {
                this.locale.send(sender, "no-permission");
                return true;
            }
            return this.handleRevive(sender, args, 0);
        }
        if (args.length == 0) {
            if (!this.hasAnyAdminPermission(sender)) {
                this.locale.send(sender, "no-permission");
                return true;
            }
            this.sendHelp(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!this.has(sender, "lifeheartspro.reload")) {
                this.locale.send(sender, "no-permission");
                return true;
            }
            this.plugin.reloadPlugin();
            this.locale.send(sender, "reload");
            return true;
        }
        if (args[0].equalsIgnoreCase("revive")) {
            if (!this.has(sender, "lifeheartspro.revive")) {
                this.locale.send(sender, "no-permission");
                return true;
            }
            return this.handleRevive(sender, args, 1);
        }
        if (args[0].equalsIgnoreCase("lives")) {
            return this.handleLives(sender, args);
        }
        if (args[0].equalsIgnoreCase("actionbar")) {
            if (!this.has(sender, "lifeheartspro.actionbar")) {
                this.locale.send(sender, "no-permission");
                return true;
            }
            return this.handleActionBar(sender, args);
        }
        if (args[0].equalsIgnoreCase("heartmode")) {
            if (!this.has(sender, "lifeheartspro.heartmode")) {
                this.locale.send(sender, "no-permission");
                return true;
            }
            return this.handleHeartMode(sender, args);
        }
        if (args[0].equalsIgnoreCase("team")) {
            return this.handleTeamAdmin(sender, args);
        }
        this.sendHelp(sender);
        return true;
    }

    private boolean handleRevive(CommandSender sender, String[] args, int n) {
        if (args.length <= n) {
            sender.sendMessage(this.locale.getPrefixed("lifehearts-usage"));
            this.locale.playSound(sender, "lifehearts-usage");
            return true;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[n]);
        if (offlinePlayer.getName() == null) {
            this.locale.send(sender, "player-not-found");
            return true;
        }
        this.lifeManager.revivePlayer(offlinePlayer);
        sender.sendMessage(
                this.locale.replace(
                        this.locale.getPrefixed("player-revived"),
                        "%player%",
                        offlinePlayer.getName()));
        this.locale.playSound(sender, "player-revived");
        if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
            this.locale.send(offlinePlayer.getPlayer(), "you-were-revived");
        }
        return true;
    }

    private boolean handleLives(CommandSender sender, String[] args) {
        int n;
        if (args.length < 4) {
            sender.sendMessage(this.locale.getPrefixed("lives-admin-usage"));
            this.locale.playSound(sender, "lives-admin-usage");
            return true;
        }
        Player player = Bukkit.getPlayerExact(args[2]);
        if (player == null) {
            this.locale.send(sender, "player-not-found");
            return true;
        }
        try {
            n = Integer.parseInt(args[3]);
        } catch (NumberFormatException numberFormatException) {
            this.locale.send(sender, "invalid-number");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (!this.has(sender, "lifeheartspro.lives." + action)) {
            this.locale.send(sender, "no-permission");
            return true;
        }
        switch (action) {
            case "set":
                {
                    this.lifeManager.setLives(player, n);
                    break;
                }
            case "add":
                {
                    this.lifeManager.addLives(player, n);
                    break;
                }
            case "remove":
                {
                    this.lifeManager.removeLives(player, n);
                    break;
                }
            default:
                {
                    sender.sendMessage(this.locale.getPrefixed("lives-action-usage"));
                    this.locale.playSound(sender, "lives-action-usage");
                    return true;
                }
        }
        sender.sendMessage(
                this.locale.replace(
                        this.locale.getPrefixed("lives-updated"),
                        "%player%",
                        player.getName(),
                        "%lives%",
                        String.valueOf(this.lifeManager.getLives(player))));
        this.locale.playSound(sender, "lives-updated");
        if (this.lifeManager.isDead(player)) {
            this.lifeManager.handleElimination(player);
        }
        return true;
    }

    private boolean handleActionBar(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.locale.getPrefixed("actionbar-usage"));
            this.locale.playSound(sender, "actionbar-usage");
            return true;
        }
        FileConfiguration fileConfiguration = this.plugin.getConfig();
        if (args[1].equalsIgnoreCase("on")) {
            fileConfiguration.set("actionbar.enabled", true);
            this.plugin.saveConfigAsync();
            this.plugin.restartRuntimeTasks();
            this.locale.send(sender, "actionbar-enabled");
            return true;
        }
        if (args[1].equalsIgnoreCase("off")) {
            fileConfiguration.set("actionbar.enabled", false);
            this.plugin.saveConfigAsync();
            this.plugin.restartRuntimeTasks();
            this.locale.send(sender, "actionbar-disabled");
            return true;
        }
        sender.sendMessage(this.locale.getPrefixed("actionbar-usage"));
        this.locale.playSound(sender, "actionbar-usage");
        return true;
    }

    private boolean handleHeartMode(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String string = this.plugin.getConfig().getString("actionbar.mode", "display");
            sender.sendMessage(
                    this.locale.replace(
                            this.locale.getPrefixed("heartmode-current"),
                            "%mode%",
                            this.prettyMode(string)));
            sender.sendMessage(this.locale.getPrefixed("heartmode-usage"));
            this.locale.playSound(sender, "heartmode-current");
            return true;
        }
        String string = args[1].toLowerCase(Locale.ROOT);
        if (!string.equals("display") && !string.equals("heart")) {
            sender.sendMessage(this.locale.getPrefixed("heartmode-invalid"));
            this.locale.playSound(sender, "heartmode-invalid");
            return true;
        }
        this.plugin.getConfig().set("actionbar.mode", string);
        this.plugin.saveConfigAsync();
        this.plugin.restartRuntimeTasks();
        sender.sendMessage(
                this.locale.replace(
                        this.locale.getPrefixed("heartmode-updated"),
                        "%mode%",
                        this.prettyMode(string)));
        this.locale.playSound(sender, "heartmode-updated");
        return true;
    }

    private String prettyMode(String string) {
        return "heart".equalsIgnoreCase(string) ? "Heart" : "Display";
    }

    private boolean handleTeamAdmin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(this.locale.getPrefixed("team-admin-usage"));
            this.locale.playSound(sender, "team-admin-usage");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("admin")) {
            return this.handleTeamAdminActions(sender, args);
        }
        String team = args[2];
        if (!this.teamManager.teamExists(team)) {
            this.locale.send(sender, "team-admin-not-found", "%team%", team);
            return true;
        }
        switch (action) {
            case "glowing":
                {
                    boolean disable;
                    if (!this.has(sender, "lifeheartspro.team.glowing")) {
                        this.locale.send(sender, "no-permission");
                        return true;
                    }
                    if (args.length < 4) {
                        sender.sendMessage(this.locale.getPrefixed("team-glowing-usage"));
                        this.locale.playSound(sender, "team-glowing-usage");
                        return true;
                    }
                    String colorInput = args[3];
                    ChatColor color = this.teamManager.parseGlowingColor(colorInput);
                    boolean bl =
                            disable =
                                    colorInput.equalsIgnoreCase("off")
                                            || colorInput.equalsIgnoreCase("none")
                                            || colorInput.equalsIgnoreCase("false")
                                            || colorInput.equalsIgnoreCase("disable");
                    if (color == null && !disable) {
                        this.locale.send(
                                sender,
                                "team-glowing-invalid",
                                "%colors%",
                                String.join(
                                        (CharSequence) ", ",
                                        this.teamManager.getGlowingColorNames()));
                        return true;
                    }
                    this.teamManager.setTeamGlowing(team, color);
                    String colorName =
                            color == null ? "off" : color.name().toLowerCase(Locale.ROOT);
                    this.locale.send(
                            sender, "team-glowing-updated", "%team%", team, "%color%", colorName);
                    return true;
                }
            case "tp":
                {
                    if (!(sender instanceof Player)) {
                        this.locale.send(sender, "players-only");
                        return true;
                    }
                    Player player = (Player) sender;
                    if (!this.has(sender, "lifeheartspro.team.tp")) {
                        this.locale.send(sender, "no-permission");
                        return true;
                    }
                    int amount = 0;
                    Location target = player.getLocation();
                    for (Player member : this.teamManager.getOnlineTeamPlayers(team)) {
                        if (member.getUniqueId().equals(player.getUniqueId())) continue;
                        member.teleport(target);
                        ++amount;
                    }
                    this.locale.send(
                            sender,
                            "team-teleported",
                            "%team%",
                            team,
                            "%amount%",
                            String.valueOf(amount));
                    return true;
                }
            case "setspawn":
                {
                    if (!(sender instanceof Player)) {
                        this.locale.send(sender, "players-only");
                        return true;
                    }
                    Player player = (Player) sender;
                    if (!this.has(sender, "lifeheartspro.team.setspawn")) {
                        this.locale.send(sender, "no-permission");
                        return true;
                    }
                    this.teamManager.setTeamSpawn(team, player.getLocation());
                    this.locale.send(sender, "team-spawn-set", "%team%", team);
                    return true;
                }
        }
        sender.sendMessage(this.locale.getPrefixed("team-admin-usage"));
        this.locale.playSound(sender, "team-admin-usage");
        return true;
    }

    private boolean handleTeamAdminActions(CommandSender sender, String[] args) {
        String action;
        if (!this.has(sender, "lifeheartspro.team.admin")) {
            this.locale.send(sender, "no-permission");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(this.locale.getPrefixed("team-admin-action-usage"));
            this.locale.playSound(sender, "team-admin-action-usage");
            return true;
        }
        switch (action = args[2].toLowerCase(Locale.ROOT)) {
            case "invite":
                {
                    if (args.length < 5) {
                        sender.sendMessage(this.locale.getPrefixed("team-admin-invite-usage"));
                        this.locale.playSound(sender, "team-admin-invite-usage");
                        return true;
                    }
                    Player target = Bukkit.getPlayerExact(args[3]);
                    String team = args[4];
                    if (target == null) {
                        this.locale.send(sender, "player-not-found");
                        return true;
                    }
                    if (!this.teamManager.teamExists(team)) {
                        this.locale.send(sender, "team-admin-not-found", "%team%", team);
                        return true;
                    }
                    if (!this.teamManager.adminAddPlayerToTeam(target, team)) {
                        this.locale.send(sender, "team-admin-action-failed");
                        return true;
                    }
                    this.locale.send(
                            sender,
                            "team-admin-added",
                            "%player%",
                            target.getName(),
                            "%team%",
                            team);
                    this.locale.send(target, "team-admin-added-target", "%team%", team);
                    return true;
                }
            case "kick":
                {
                    if (args.length < 5) {
                        sender.sendMessage(this.locale.getPrefixed("team-admin-kick-usage"));
                        this.locale.playSound(sender, "team-admin-kick-usage");
                        return true;
                    }
                    Player target = Bukkit.getPlayerExact(args[3]);
                    String team = args[4];
                    if (target == null) {
                        this.locale.send(sender, "player-not-found");
                        return true;
                    }
                    if (!this.teamManager.teamExists(team)) {
                        this.locale.send(sender, "team-admin-not-found", "%team%", team);
                        return true;
                    }
                    if (!this.teamManager.adminKickPlayerFromTeam(target, team)) {
                        this.locale.send(sender, "team-admin-action-failed");
                        return true;
                    }
                    this.locale.send(
                            sender,
                            "team-admin-kicked",
                            "%player%",
                            target.getName(),
                            "%team%",
                            team);
                    this.locale.send(target, "team-admin-kicked-target", "%team%", team);
                    return true;
                }
            case "disband":
                {
                    String team = args[3];
                    if (!this.teamManager.teamExists(team)) {
                        this.locale.send(sender, "team-admin-not-found", "%team%", team);
                        return true;
                    }
                    if (!this.teamManager.adminDisbandTeam(team)) {
                        this.locale.send(sender, "team-admin-action-failed");
                        return true;
                    }
                    this.locale.send(sender, "team-admin-disbanded", "%team%", team);
                    return true;
                }
        }
        sender.sendMessage(this.locale.getPrefixed("team-admin-action-usage"));
        this.locale.playSound(sender, "team-admin-action-usage");
        return true;
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("lifeheartspro.admin");
    }

    private boolean hasAnyAdminPermission(CommandSender sender) {
        return sender.hasPermission("lifeheartspro.admin")
                || sender.hasPermission("lifeheartspro.reload")
                || sender.hasPermission("lifeheartspro.revive")
                || sender.hasPermission("lifeheartspro.lives.set")
                || sender.hasPermission("lifeheartspro.lives.add")
                || sender.hasPermission("lifeheartspro.lives.remove")
                || sender.hasPermission("lifeheartspro.actionbar")
                || sender.hasPermission("lifeheartspro.heartmode")
                || sender.hasPermission("lifeheartspro.team.glowing")
                || sender.hasPermission("lifeheartspro.team.tp")
                || sender.hasPermission("lifeheartspro.team.setspawn")
                || sender.hasPermission("lifeheartspro.team.admin");
    }

    public List<String> onTabComplete(
            CommandSender sender, Command command, String string, String[] args) {
        ArrayList<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("reload");
            completions.add("revive");
            completions.add("lives");
            completions.add("actionbar");
            completions.add("heartmode");
            completions.add("team");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("lives")) {
            completions.add("set");
            completions.add("add");
            completions.add("remove");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("revive")) {
            Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("lives")) {
            Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("actionbar")) {
            completions.add("on");
            completions.add("off");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("heartmode")) {
            completions.add("display");
            completions.add("heart");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("team")) {
            completions.add("glowing");
            completions.add("tp");
            completions.add("setspawn");
            completions.add("admin");
        }
        if (args.length == 3
                && args[0].equalsIgnoreCase("team")
                && args[1].equalsIgnoreCase("admin")) {
            completions.add("invite");
            completions.add("kick");
            completions.add("disband");
        }
        if (args.length == 3
                && args[0].equalsIgnoreCase("team")
                && !args[1].equalsIgnoreCase("admin")) {
            completions.addAll(this.teamManager.getSortedTeams());
        }
        if (args.length == 4
                && args[0].equalsIgnoreCase("team")
                && args[1].equalsIgnoreCase("glowing")) {
            completions.addAll(this.teamManager.getGlowingColorNames());
        }
        if (args.length == 4
                && args[0].equalsIgnoreCase("team")
                && args[1].equalsIgnoreCase("admin")) {
            if (args[2].equalsIgnoreCase("invite") || args[2].equalsIgnoreCase("kick")) {
                Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
            } else if (args[2].equalsIgnoreCase("disband")) {
                completions.addAll(this.teamManager.getSortedTeams());
            }
        }
        if (args.length == 5
                && args[0].equalsIgnoreCase("team")
                && args[1].equalsIgnoreCase("admin")
                && (args[2].equalsIgnoreCase("invite") || args[2].equalsIgnoreCase("kick"))) {
            completions.addAll(this.teamManager.getSortedTeams());
        }
        return completions;
    }
}
