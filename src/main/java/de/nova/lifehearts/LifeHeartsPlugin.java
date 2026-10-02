package de.nova.lifehearts;

import de.nova.lifehearts.command.HeartCommand;
import de.nova.lifehearts.command.LifeHeartsCommand;
import de.nova.lifehearts.command.LivesCommand;
import de.nova.lifehearts.command.TeamCommand;
import de.nova.lifehearts.listener.PlayerDeathListener;
import de.nova.lifehearts.listener.PlayerJoinListener;
import de.nova.lifehearts.listener.TeamDamageListener;
import de.nova.lifehearts.listener.TeamRespawnListener;
import de.nova.lifehearts.manager.DataManager;
import de.nova.lifehearts.manager.LifeManager;
import de.nova.lifehearts.manager.LocaleManager;
import de.nova.lifehearts.manager.SoundManager;
import de.nova.lifehearts.manager.TeamManager;
import de.nova.lifehearts.task.ActionBarTask;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class LifeHeartsPlugin extends JavaPlugin {
    private static LifeHeartsPlugin instance;
    private LocaleManager localeManager;
    private SoundManager soundManager;
    private DataManager dataManager;
    private LifeManager lifeManager;
    private TeamManager teamManager;
    private BukkitTask actionBarTask;
    private BukkitTask glowingTask;

    public static LifeHeartsPlugin getInstance() {
        return instance;
    }

    public LocaleManager getLocaleManager() {
        return this.localeManager;
    }

    public SoundManager getSoundManager() {
        return this.soundManager;
    }

    public DataManager getDataManager() {
        return this.dataManager;
    }

    public LifeManager getLifeManager() {
        return this.lifeManager;
    }

    public TeamManager getTeamManager() {
        return this.teamManager;
    }

    public void onEnable() {
        instance = this;
        this.getLogger().info("Starting LifeHeartsPro...");
        this.setupFiles();
        boolean correctedConfig = this.validateConfig();
        this.soundManager = new SoundManager(this);
        this.localeManager = new LocaleManager(this);
        this.dataManager = new DataManager(this);
        if (correctedConfig) {
            this.saveConfigAsync();
        }
        this.lifeManager = new LifeManager(this);
        this.teamManager = new TeamManager(this);
        this.registerCommands();
        this.registerEvents();
        this.startActionBar();
        this.registerPlaceholderAPI();
        new UpdateChecker(this).checkAsync();
        this.getLogger().info("LifeHeartsPro enabled.");
    }

    public void onDisable() {
        this.stopRuntimeTasks();
        if (this.teamManager != null) {
            this.teamManager.shutdown();
        }
        if (this.dataManager != null) {
            this.dataManager.close();
        }
        instance = null;
        this.getLogger().info("LifeHeartsPro disabled.");
    }

    private void setupFiles() {
        this.saveDefaultConfig();
        this.reloadConfig();
        this.ensureConfigDefaults();
        File file = new File(this.getDataFolder(), "lang");
        if (!file.exists()) {
            file.mkdirs();
        }
        this.createLangFile("en_us.yml");
        this.createLangFile("de_eu.yml");
        this.createLangFile("es_es.yml");
        this.createLangFile("fr_fr.yml");
        this.createResourceFile("sounds.yml");
        this.updateConfigFile();
        this.updateResourceFile("sounds.yml");
        this.updateResourceFile("lang/en_us.yml");
        this.updateResourceFile("lang/de_eu.yml");
        this.updateResourceFile("lang/es_es.yml");
        this.updateResourceFile("lang/fr_fr.yml");
    }

    private void ensureConfigDefaults() {
        this.getConfig().addDefault("locale", "en_us");
        this.getConfig().addDefault("lives.default", 3);
        this.getConfig().addDefault("lives.max", 3);
        this.getConfig().addDefault("lives.set-online-lives-to-default-on-reload", false);
        this.getConfig().addDefault("lives.min", 0);
        this.getConfig().addDefault("lives.lose-life-on-death", true);
        this.getConfig().addDefault("lives.only-lose-life-when-killed-by-player", true);
        this.getConfig().addDefault("lives.ban-on-zero-lives", true);
        this.getConfig().addDefault("heart-transfer.enabled", true);
        this.getConfig().addDefault("heart-transfer.minimum-hearts-after-transfer", 1);
        this.getConfig().addDefault("teams.enabled", true);
        this.getConfig().addDefault("teams.max-members", 5);
        this.getConfig().addDefault("teams.name.min-length", 3);
        this.getConfig().addDefault("teams.name.max-length", 16);
        this.getConfig().addDefault("teams.friendly-fire", false);
        this.getConfig().addDefault("teams.allow-player-teams", true);
        this.getConfig().addDefault("teams.allow-leave", true);
        this.getConfig().addDefault("teams.allow-kick", true);
        this.getConfig().addDefault("teams.allow-disband", true);
        this.getConfig().addDefault("teams.glowing.enabled", true);
        this.getConfig().addDefault("teams.enemy-glowing.enabled", false);
        this.getConfig().addDefault("teams.enemy-glowing.color", "dark_red");
        this.getConfig().addDefault("teams.glowing.force-main-scoreboard", false);
        this.getConfig().addDefault("teams.glowing.sync-interval-ticks", 100);
        this.getConfig().addDefault("actionbar.enabled", true);
        this.getConfig().addDefault("actionbar.update-interval", 40);
        this.getConfig().addDefault("actionbar.mode", "display");
        this.getConfig().addDefault("actionbar.heart-symbol", "\u2764");
        this.getConfig().addDefault("actionbar.gray-heart-symbol", "\u2764");
        this.getConfig()
                .addDefault(
                        "actionbar.display-format",
                        "&c\u2764 &fLives: &a%lives% &c\u2764 &8| &bTeam: &f%team%");
        this.getConfig()
                .addDefault(
                        "actionbar.heart-format",
                        "&c%red_hearts%&7%gray_hearts% &8| &bTeam: &f%team%");
        this.getConfig().addDefault("actionbar.show-lives", true);
        this.getConfig().addDefault("actionbar.show-team", true);
        this.getConfig().addDefault("join-message.enabled", true);
        this.getConfig().addDefault("join-message.show-lives", true);
        this.getConfig().addDefault("join-message.show-team", true);
        this.getConfig().addDefault("update-checker.enabled", false);
        this.getConfig().addDefault("storage.flush-delay-ticks", 20);
        this.getConfig().addDefault("debug.enabled", false);
        this.getConfig().options().copyDefaults(false);
    }

    private boolean validateConfig() {
        FileConfiguration config = this.getConfig();
        boolean changed = false;
        changed |= this.clampInt(config, "lives.min", 0, 100, 0);
        int minimumLives = config.getInt("lives.min", 0);
        changed |= this.clampInt(config, "lives.max", minimumLives, 100, Math.max(3, minimumLives));
        int maximumLives = config.getInt("lives.max", 3);
        changed |=
                this.clampInt(
                        config,
                        "lives.default",
                        minimumLives,
                        maximumLives,
                        Math.min(3, maximumLives));
        changed |=
                this.clampInt(
                        config,
                        "heart-transfer.minimum-hearts-after-transfer",
                        minimumLives,
                        maximumLives,
                        Math.max(1, minimumLives));
        changed |= this.clampInt(config, "teams.max-members", 1, 500, 5);
        changed |= this.clampInt(config, "teams.name.min-length", 1, 32, 3);
        int minimumName = config.getInt("teams.name.min-length", 3);
        changed |=
                this.clampInt(
                        config,
                        "teams.name.max-length",
                        minimumName,
                        32,
                        Math.max(16, minimumName));
        changed |= this.clampLong(config, "actionbar.update-interval", 10L, 1200L, 40L);
        changed |= this.clampLong(config, "teams.glowing.sync-interval-ticks", 20L, 1200L, 100L);
        changed |= this.clampLong(config, "storage.flush-delay-ticks", 1L, 200L, 20L);
        return changed;
    }

    private boolean clampInt(
            FileConfiguration config, String path, int minimum, int maximum, int fallback) {
        int value = config.getInt(path, fallback);
        int corrected = Math.max(minimum, Math.min(maximum, value));
        if (value == corrected) {
            return false;
        }
        config.set(path, corrected);
        this.getLogger().warning("Corrected " + path + " from " + value + " to " + corrected + '.');
        return true;
    }

    private boolean clampLong(
            FileConfiguration config, String path, long minimum, long maximum, long fallback) {
        long value = config.getLong(path, fallback);
        long corrected = Math.max(minimum, Math.min(maximum, value));
        if (value == corrected) {
            return false;
        }
        config.set(path, corrected);
        this.getLogger().warning("Corrected " + path + " from " + value + " to " + corrected + '.');
        return true;
    }

    private void updateConfigFile() {
        File file = new File(this.getDataFolder(), "config.yml");
        if (!file.exists()) {
            return;
        }
        try (InputStreamReader reader =
                new InputStreamReader(this.getResource("config.yml"), StandardCharsets.UTF_8); ) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
            if (this.copyMissingDefaults(
                    (ConfigurationSection) current, (ConfigurationSection) defaults)) {
                current.save(file);
                this.reloadConfig();
                this.getLogger().info("Updated config.yml with missing default keys.");
            }
        } catch (IOException | RuntimeException exception) {
            this.getLogger()
                    .warning("Could not update config.yml defaults: " + exception.getMessage());
        }
    }

    private void createLangFile(String string) {
        this.createResourceFile("lang/" + string);
    }

    private void createResourceFile(String string) {
        File file = new File(this.getDataFolder(), string);
        if (!file.exists()) {
            this.getLogger().info("Creating resource file " + string);
            this.saveResource(string, false);
        }
    }

    private void updateResourceFile(String resourcePath) {
        File file = new File(this.getDataFolder(), resourcePath);
        if (!file.exists()) {
            this.createResourceFile(resourcePath);
            return;
        }
        try (InputStreamReader reader =
                new InputStreamReader(this.getResource(resourcePath), StandardCharsets.UTF_8); ) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
            if (this.copyMissingDefaults(
                    (ConfigurationSection) current, (ConfigurationSection) defaults)) {
                current.save(file);
                this.getLogger().info("Updated " + resourcePath + " with missing default keys.");
            }
        } catch (IOException | RuntimeException exception) {
            this.getLogger()
                    .warning("Could not update " + resourcePath + ": " + exception.getMessage());
        }
    }

    private boolean copyMissingDefaults(
            ConfigurationSection current, ConfigurationSection defaults) {
        boolean changed = false;
        for (String path : defaults.getKeys(false)) {
            String key = path;
            Object value = defaults.get(key);
            if (value instanceof ConfigurationSection) {
                ConfigurationSection defaultSection = (ConfigurationSection) value;
                ConfigurationSection currentSection = current.getConfigurationSection(path);
                if (currentSection == null) {
                    current.createSection(path);
                    currentSection = current.getConfigurationSection(path);
                    changed = true;
                }
                if (currentSection == null
                        || !this.copyMissingDefaults(currentSection, defaultSection)) continue;
                changed = true;
                continue;
            }
            if (current.contains(path)) continue;
            current.set(path, value);
            changed = true;
        }
        return changed;
    }

    private void registerCommands() {
        PluginCommand pluginCommand = this.getCommand("lifeheartspro");
        if (pluginCommand != null) {
            LifeHeartsCommand lifeHeartsCommand = new LifeHeartsCommand(this);
            pluginCommand.setExecutor(lifeHeartsCommand);
            pluginCommand.setTabCompleter(lifeHeartsCommand);
        } else {
            this.getLogger().warning("Command lifeheartspro not found!");
        }
        PluginCommand reviveCommand = this.getCommand("revive");
        if (reviveCommand != null) {
            reviveCommand.setExecutor(new LifeHeartsCommand(this));
        } else {
            this.getLogger().warning("Command revive not found!");
        }
        PluginCommand pluginCommand2 = this.getCommand("heart");
        if (pluginCommand2 != null) {
            HeartCommand heartCommand = new HeartCommand(this);
            pluginCommand2.setExecutor(heartCommand);
            pluginCommand2.setTabCompleter(heartCommand);
        } else {
            this.getLogger().warning("Command heart not found!");
        }
        PluginCommand teamPluginCommand = this.getCommand("team");
        if (teamPluginCommand != null) {
            TeamCommand teamCommand = new TeamCommand(this);
            teamPluginCommand.setExecutor(teamCommand);
            teamPluginCommand.setTabCompleter(teamCommand);
        } else {
            this.getLogger().warning("Command team not found!");
        }
        PluginCommand livesCommand = this.getCommand("lives");
        if (livesCommand != null) {
            LivesCommand command = new LivesCommand(this);
            livesCommand.setExecutor(command);
            livesCommand.setTabCompleter(command);
        } else {
            this.getLogger().warning("Command lives not found!");
        }
    }

    private void registerEvents() {
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        this.getServer().getPluginManager().registerEvents(new TeamDamageListener(this), this);
        this.getServer().getPluginManager().registerEvents(new TeamRespawnListener(this), this);
    }

    private void startActionBar() {
        this.stopRuntimeTasks();
        if (this.getConfig().getBoolean("actionbar.enabled")) {
            long interval =
                    Math.max(
                            10L,
                            Math.min(
                                    1200L,
                                    this.getConfig().getLong("actionbar.update-interval", 40L)));
            this.actionBarTask = new ActionBarTask(this).runTaskTimer(this, 20L, interval);
            this.getLogger().info("ActionBar task started with interval " + interval + " ticks.");
        }
        if (this.getConfig().getBoolean("teams.glowing.enabled", true)) {
            long syncInterval =
                    Math.max(
                            20L,
                            Math.min(
                                    1200L,
                                    this.getConfig()
                                            .getLong("teams.glowing.sync-interval-ticks", 100L)));
            this.glowingTask =
                    Bukkit.getScheduler()
                            .runTaskTimer(
                                    this,
                                    () -> this.teamManager.syncAllGlowing(),
                                    40L,
                                    syncInterval);
            this.getLogger()
                    .info(
                            "Team glowing sync task started with interval "
                                    + syncInterval
                                    + " ticks.");
        }
    }

    private void stopRuntimeTasks() {
        if (this.actionBarTask != null) {
            this.actionBarTask.cancel();
            this.actionBarTask = null;
        }
        if (this.glowingTask != null) {
            this.glowingTask.cancel();
            this.glowingTask = null;
        }
    }

    public void restartRuntimeTasks() {
        this.startActionBar();
    }

    public void saveConfigAsync() {
        this.dataManager.saveConfiguration(
                this.getDataFolder().toPath().resolve("config.yml"),
                this.getConfig().saveToString());
    }

    private void registerPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                Class<?> clazz =
                        Class.forName("de.nova.lifehearts.placeholder.LifeHeartsExpansion");
                Object expansion = clazz.getConstructor(LifeHeartsPlugin.class).newInstance(this);
                clazz.getMethod("register").invoke(expansion);
                this.getLogger().info("PlaceholderAPI expansion registered.");
            } catch (Exception exception) {
                this.getLogger()
                        .warning(
                                "PlaceholderAPI expansion could not be registered: "
                                        + exception.getMessage());
            }
        } else {
            this.getLogger().info("PlaceholderAPI not found, skipping...");
        }
    }

    public void reloadPlugin() {
        this.dataManager.flush();
        this.reloadConfig();
        this.ensureConfigDefaults();
        boolean correctedConfig = this.validateConfig();
        this.soundManager.reload();
        this.localeManager.reload();
        this.dataManager.load();
        if (correctedConfig) {
            this.saveConfigAsync();
        }
        this.lifeManager.clearCache();
        if (this.getConfig().getBoolean("lives.set-online-lives-to-default-on-reload", false)) {
            this.lifeManager.resetAllOnlineLivesToDefault();
        } else {
            this.lifeManager.clampAllOnlineLivesToBounds();
        }
        this.teamManager.reloadPersistentData();
        this.startActionBar();
        this.getLogger().info("LifeHeartsPro reloaded.");
    }
}
