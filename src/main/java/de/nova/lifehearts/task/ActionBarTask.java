package de.nova.lifehearts.task;

import de.nova.lifehearts.LifeHeartsPlugin;
import de.nova.lifehearts.manager.LifeManager;
import de.nova.lifehearts.manager.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/** Sends a cached action-bar template at one shared interval for all online players. */
public final class ActionBarTask extends BukkitRunnable {
    private static final String DEFAULT_DISPLAY = "&c❤ &fLives: &a%lives% &c❤ &8| &bTeam: &f%team%";
    private static final String DEFAULT_HEART =
            "&c%red_hearts%&7%gray_hearts% &8| &bTeam: &f%team%";
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final LifeManager lifeManager;
    private final TeamManager teamManager;
    private final String mode;
    private final String displayFormat;
    private final String heartFormat;
    private final String redHeart;
    private final String grayHeart;
    private final String noTeam;
    private final int maximumLives;

    public ActionBarTask(LifeHeartsPlugin plugin) {
        this.lifeManager = plugin.getLifeManager();
        this.teamManager = plugin.getTeamManager();
        this.mode = plugin.getConfig().getString("actionbar.mode", "display");
        this.maximumLives = lifeManager.getMaximumLives();
        this.redHeart =
                normalizeSymbol(plugin.getConfig().getString("actionbar.heart-symbol", "❤"));
        this.grayHeart =
                normalizeSymbol(plugin.getConfig().getString("actionbar.gray-heart-symbol", "❤"));
        String configuredDisplay =
                plugin.getConfig().getString("actionbar.display-format", DEFAULT_DISPLAY);
        this.displayFormat =
                configuredDisplay == null
                                || configuredDisplay.isBlank()
                                || DEFAULT_DISPLAY.equals(configuredDisplay)
                        ? plugin.getLocaleManager().getRaw("actionbar")
                        : configuredDisplay;
        String configuredHeart =
                plugin.getConfig().getString("actionbar.heart-format", DEFAULT_HEART);
        this.heartFormat =
                configuredHeart == null
                                || configuredHeart.isBlank()
                                || DEFAULT_HEART.equals(configuredHeart)
                        ? plugin.getLocaleManager().getRaw("actionbar-heart")
                        : configuredHeart;
        this.noTeam = plugin.getLocaleManager().get("team-none");
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int lives = lifeManager.getLives(player);
            String team = teamManager.getTeam(player);
            String rendered =
                    "heart".equalsIgnoreCase(mode)
                            ? renderHearts(lives, team == null ? noTeam : team)
                            : displayFormat
                                    .replace("%lives%", Integer.toString(lives))
                                    .replace("%team%", team == null ? noTeam : team);
            Component component = LEGACY.deserialize(rendered);
            player.sendActionBar(component);
        }
    }

    private String renderHearts(int lives, String team) {
        int active = Math.max(0, Math.min(maximumLives, lives));
        int missing = Math.max(0, maximumLives - active);
        String red = redHeart.repeat(active);
        String gray = grayHeart.repeat(missing);
        return heartFormat
                .replace("%hearts%", red + gray)
                .replace("%red_hearts%", red)
                .replace("%gray_hearts%", gray)
                .replace("%lives%", Integer.toString(lives))
                .replace("%team%", team);
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return "❤";
        }
        String normalized = symbol.replace("️", "");
        return normalized.isBlank() ? "❤" : normalized;
    }
}
