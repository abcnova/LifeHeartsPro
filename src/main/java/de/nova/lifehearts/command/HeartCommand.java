package de.nova.lifehearts.command;

import de.nova.lifehearts.LifeHeartsPlugin;
import de.nova.lifehearts.manager.LifeManager;
import de.nova.lifehearts.manager.LocaleManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/** Handles player-to-player life transfers. */
public final class HeartCommand implements CommandExecutor, TabCompleter {
    private final LifeHeartsPlugin plugin;
    private final LifeManager lifeManager;
    private final LocaleManager locale;

    public HeartCommand(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
        this.lifeManager = plugin.getLifeManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            locale.send(sender, "players-only");
            return true;
        }
        if (!has(player, "lifeheartspro.heart.give")) {
            locale.send(player, "no-permission");
            return true;
        }
        if (!plugin.getConfig().getBoolean("heart-transfer.enabled", true)) {
            locale.send(player, "heart-transfer-failed");
            return true;
        }
        if (args.length != 3 || !args[0].equalsIgnoreCase("give")) {
            locale.send(player, "heart-usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            locale.send(player, "player-not-found");
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException ignored) {
            locale.send(player, "invalid-number");
            return true;
        }
        if (amount <= 0) {
            locale.send(player, "invalid-number");
            return true;
        }
        int minimumAfter =
                plugin.getConfig().getInt("heart-transfer.minimum-hearts-after-transfer", 1);
        if (!lifeManager.transferLives(player, target, amount, minimumAfter)) {
            locale.send(player, "heart-transfer-failed");
            return true;
        }
        locale.send(
                player,
                "heart-given",
                "%player%",
                target.getName(),
                "%amount%",
                Integer.toString(amount));
        locale.send(
                target,
                "heart-received",
                "%player%",
                player.getName(),
                "%amount%",
                Integer.toString(amount));
        return true;
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("lifeheartspro.admin");
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return startsWith(List.of("give"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getUniqueId()
                        .equals(sender instanceof Player source ? source.getUniqueId() : null)) {
                    players.add(player.getName());
                }
            }
            return startsWith(players, args[1]);
        }
        return List.of();
    }

    private List<String> startsWith(List<String> values, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
