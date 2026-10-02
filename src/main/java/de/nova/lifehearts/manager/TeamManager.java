package de.nova.lifehearts.manager;

import de.nova.lifehearts.LifeHeartsPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TeamManager {
    private final LifeHeartsPlugin plugin;
    private final Map<String, UUID> owners = new HashMap<>();
    private final Map<String, Set<UUID>> members = new HashMap<>();
    private final Map<UUID, String> teamByMember = new HashMap<>();
    private final Map<String, String> displayNames = new HashMap<>();
    private final Map<UUID, String> invites = new HashMap<>();
    private final Map<String, ChatColor> glowingColors = new HashMap<>();
    private final Map<UUID, Scoreboard> viewerGlowScoreboards = new HashMap<>();
    private boolean glowStateActive;
    private static final List<String> GLOWING_COLOR_NAMES =
            List.of(
                    "black",
                    "dark_blue",
                    "dark_green",
                    "dark_aqua",
                    "dark_red",
                    "dark_purple",
                    "gold",
                    "gray",
                    "dark_gray",
                    "blue",
                    "green",
                    "aqua",
                    "red",
                    "light_purple",
                    "yellow",
                    "white",
                    "off");

    public TeamManager(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
        this.loadPersistentData();
    }

    public boolean createTeam(Player owner, String name) {
        if (!this.isValidTeamName(name)) {
            return false;
        }
        String key = this.normalize(name);
        if (this.members.containsKey(key)) {
            return false;
        }
        if (!this.plugin.getConfig().getBoolean("teams.enabled", true)) {
            return false;
        }
        this.leaveTeam(owner);
        HashSet<UUID> set = new HashSet<>();
        set.add(owner.getUniqueId());
        this.members.put(key, set);
        this.teamByMember.put(owner.getUniqueId(), key);
        this.displayNames.put(key, name.trim());
        this.owners.put(key, owner.getUniqueId());
        this.saveTeam(key);
        this.applyGlowing(owner);
        return true;
    }

    public String getTeam(Player player) {
        String key = this.getTeamKey(player);
        return key == null ? null : this.getDisplayName(key);
    }

    private String getTeamKey(Player player) {
        return player == null ? null : this.teamByMember.get(player.getUniqueId());
    }

    public boolean isOwner(Player player) {
        String team = this.getTeamKey(player);
        if (team == null) {
            return false;
        }
        UUID owner = this.owners.get(team);
        return owner != null && owner.equals(player.getUniqueId());
    }

    public boolean invite(Player owner, Player target) {
        if (owner == null || target == null) {
            return false;
        }
        String team = this.getTeamKey(owner);
        if (team == null) {
            return false;
        }
        UUID ownerUuid = this.owners.get(team);
        if (ownerUuid == null || !ownerUuid.equals(owner.getUniqueId())) {
            return false;
        }
        if (this.isInAnyTeam(target)) {
            return false;
        }
        int maxMembers = this.plugin.getConfig().getInt("teams.max-members", 5);
        if (this.getTeamSize(team) >= maxMembers) {
            return false;
        }
        this.invites.put(target.getUniqueId(), team);
        return true;
    }

    public boolean acceptInvite(Player player) {
        if (player == null) {
            return false;
        }
        String team = this.invites.remove(player.getUniqueId());
        if (team == null) {
            return false;
        }
        Set<UUID> teamMembers = this.members.get(team);
        if (teamMembers == null) {
            return false;
        }
        int maxMembers = this.plugin.getConfig().getInt("teams.max-members", 5);
        if (teamMembers.size() >= maxMembers) {
            return false;
        }
        this.leaveTeam(player);
        teamMembers.add(player.getUniqueId());
        this.teamByMember.put(player.getUniqueId(), team);
        this.saveTeam(team);
        this.applyGlowing(player);
        return true;
    }

    public void leaveTeam(Player player) {
        if (player == null) {
            return;
        }
        String team = this.getTeamKey(player);
        if (team == null) {
            return;
        }
        Set<UUID> teamMembers = this.members.get(team);
        if (teamMembers == null) {
            return;
        }
        teamMembers.remove(player.getUniqueId());
        this.teamByMember.remove(player.getUniqueId());
        this.clearGlowing(player);
        UUID ownerUuid = this.owners.get(team);
        if (ownerUuid != null && ownerUuid.equals(player.getUniqueId())) {
            this.removeTeam(team);
            return;
        }
        if (teamMembers.isEmpty()) {
            this.removeTeam(team);
        } else {
            this.saveTeam(team);
        }
    }

    public boolean kick(Player owner, Player target) {
        if (owner == null || target == null) {
            return false;
        }
        String team = this.getTeamKey(owner);
        if (team == null) {
            return false;
        }
        UUID ownerUuid = this.owners.get(team);
        if (ownerUuid == null || !ownerUuid.equals(owner.getUniqueId())) {
            return false;
        }
        if (owner.getUniqueId().equals(target.getUniqueId())) {
            return false;
        }
        Set<UUID> teamMembers = this.members.get(team);
        if (teamMembers == null || !teamMembers.contains(target.getUniqueId())) {
            return false;
        }
        teamMembers.remove(target.getUniqueId());
        this.teamByMember.remove(target.getUniqueId());
        this.saveTeam(team);
        this.clearGlowing(target);
        return true;
    }

    public boolean disband(Player owner) {
        if (owner == null) {
            return false;
        }
        String team = this.getTeamKey(owner);
        if (team == null) {
            return false;
        }
        UUID ownerUuid = this.owners.get(team);
        if (ownerUuid == null || !ownerUuid.equals(owner.getUniqueId())) {
            return false;
        }
        this.removeTeam(team);
        return true;
    }

    public boolean adminAddPlayerToTeam(Player target, String team) {
        if (target == null || !this.teamExists(team)) {
            return false;
        }
        String key = this.normalize(team);
        Set<UUID> teamMembers = this.members.get(key);
        int maxMembers = this.plugin.getConfig().getInt("teams.max-members", 5);
        if (teamMembers == null || teamMembers.size() >= maxMembers) {
            return false;
        }
        this.leaveTeam(target);
        teamMembers.add(target.getUniqueId());
        this.teamByMember.put(target.getUniqueId(), key);
        this.saveTeam(key);
        this.applyGlowing(target);
        return true;
    }

    public boolean adminKickPlayerFromTeam(Player target, String team) {
        if (target == null || !this.teamExists(team)) {
            return false;
        }
        String key = this.normalize(team);
        Set<UUID> teamMembers = this.members.get(key);
        if (teamMembers == null || !teamMembers.remove(target.getUniqueId())) {
            return false;
        }
        this.teamByMember.remove(target.getUniqueId());
        UUID ownerUuid = this.owners.get(key);
        if (ownerUuid != null && ownerUuid.equals(target.getUniqueId())) {
            this.removeTeam(key);
            return true;
        }
        this.invites.remove(target.getUniqueId());
        this.saveTeam(key);
        this.clearGlowing(target);
        return true;
    }

    public boolean adminDisbandTeam(String team) {
        if (!this.teamExists(team)) {
            return false;
        }
        this.removeTeam(this.normalize(team));
        return true;
    }

    public boolean canDamage(Player attacker, Player victim) {
        String teamA = this.getTeamKey(attacker);
        String teamB = this.getTeamKey(victim);
        if (teamA == null || teamB == null) {
            return true;
        }
        if (!teamA.equalsIgnoreCase(teamB)) {
            return true;
        }
        return this.plugin.getConfig().getBoolean("teams.friendly-fire", false);
    }

    public Set<String> getTeamMembers(String team) {
        HashSet<String> result = new HashSet<>();
        if (team == null) {
            return result;
        }
        Set<UUID> set = this.members.get(this.normalize(team));
        if (set == null) {
            return result;
        }
        for (UUID uuid : set) {
            Player online = Bukkit.getPlayer((UUID) uuid);
            if (online != null) {
                result.add(online.getName());
                continue;
            }
            result.add(uuid.toString());
        }
        return result;
    }

    public Set<String> getTeams() {
        HashSet<String> result = new HashSet<>();
        for (String team : this.members.keySet()) {
            result.add(this.getDisplayName(team));
        }
        return result;
    }

    public List<String> getSortedTeams() {
        ArrayList<String> list = new ArrayList<String>(this.getTeams());
        Collections.sort(list);
        return list;
    }

    public int getTeamSize(String team) {
        if (team == null) {
            return 0;
        }
        Set<UUID> set = this.members.get(this.normalize(team));
        return set == null ? 0 : set.size();
    }

    public boolean isInAnyTeam(Player player) {
        return this.getTeam(player) != null;
    }

    public boolean hasInvite(Player player) {
        return this.invites.containsKey(player.getUniqueId());
    }

    public List<Player> getOnlineTeamPlayers(String team) {
        ArrayList<Player> result = new ArrayList<>();
        if (team == null) {
            return result;
        }
        Set<UUID> set = this.members.get(this.normalize(team));
        if (set == null) {
            return result;
        }
        for (UUID uuid : set) {
            Player player = Bukkit.getPlayer((UUID) uuid);
            if (player == null || !player.isOnline()) continue;
            result.add(player);
        }
        return result;
    }

    public boolean teamExists(String team) {
        return team != null && this.members.containsKey(this.normalize(team));
    }

    public boolean setTeamGlowing(String team, ChatColor color) {
        if (!this.teamExists(team)) {
            return false;
        }
        String key = this.normalize(team);
        if (color == null) {
            this.glowingColors.remove(key);
            this.plugin.getDataManager().getConfig().set("teams." + key + ".glowing", null);
            this.plugin.getDataManager().save();
            this.clearTeamGlowing(key);
            this.saveTeam(key);
            return true;
        }
        this.glowingColors.put(key, color);
        this.plugin.getDataManager().getConfig().set("teams." + key + ".glowing", color.name());
        this.plugin.getDataManager().save();
        this.applyTeamGlowing(key);
        return true;
    }

    public ChatColor parseGlowingColor(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("off")
                || normalized.equals("none")
                || normalized.equals("false")
                || normalized.equals("disable")) {
            return null;
        }
        if (normalized.length() == 2
                && (normalized.charAt(0) == '&' || normalized.charAt(0) == '\u00a7')) {
            ChatColor byCode = ChatColor.getByChar((char) normalized.charAt(1));
            return byCode != null && byCode.isColor() ? byCode : null;
        }
        if (normalized.length() == 1) {
            ChatColor byCode = ChatColor.getByChar((char) normalized.charAt(0));
            return byCode != null && byCode.isColor() ? byCode : null;
        }
        try {
            ChatColor color = ChatColor.valueOf(normalized.toUpperCase(Locale.ROOT));
            return color.isColor() ? color : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public List<String> getGlowingColorNames() {
        return GLOWING_COLOR_NAMES;
    }

    public void applyGlowing(Player player) {
        this.syncGlowingForTarget(player);
    }

    public void handleJoin(Player player) {
        if (!this.isGlowingEnabled()) {
            return;
        }
        this.syncGlowingForViewer(player);
        this.syncGlowingForTarget(player);
    }

    public void handleQuit(Player player) {
        this.invites.remove(player.getUniqueId());
        this.viewerGlowScoreboards.remove(player.getUniqueId());
        this.clearGlowing(player);
    }

    public void shutdown() {
        this.clearAllLifeHeartsGlowing();
        this.invites.clear();
        this.viewerGlowScoreboards.clear();
    }

    public void applyAllGlowing() {
        this.syncAllGlowing();
    }

    public void syncAllGlowing() {
        this.cleanupStaleGlowScoreboards();
        if (!this.isGlowingEnabled()) {
            if (this.glowStateActive) {
                this.clearAllLifeHeartsGlowing();
            }
            return;
        }
        if (this.glowingColors.isEmpty() && !this.isEnemyGlowingEnabled()) {
            if (this.glowStateActive) {
                this.clearAllLifeHeartsGlowing();
            }
            return;
        }
        this.glowStateActive = true;
        if (this.shouldUseMainScoreboardMode()) {
            this.releaseViewerGlowScoreboards();
            this.syncMainScoreboardGlowing();
            return;
        }
        if (!this.isEnemyGlowingEnabled()) {
            this.releaseViewerGlowScoreboards();
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            this.syncGlowingForViewer(viewer);
        }
    }

    public void reloadPersistentData() {
        this.members.clear();
        this.teamByMember.clear();
        this.owners.clear();
        this.displayNames.clear();
        this.glowingColors.clear();
        this.invites.clear();
        this.loadPersistentData();
        this.applyAllGlowing();
    }

    public void setTeamSpawn(String team, Location location) {
        if (!this.teamExists(team) || location == null || location.getWorld() == null) {
            return;
        }
        String key = this.normalize(team);
        this.plugin
                .getDataManager()
                .getConfig()
                .set("teams." + key + ".spawn.world", location.getWorld().getName());
        this.plugin.getDataManager().getConfig().set("teams." + key + ".spawn.x", location.getX());
        this.plugin.getDataManager().getConfig().set("teams." + key + ".spawn.y", location.getY());
        this.plugin.getDataManager().getConfig().set("teams." + key + ".spawn.z", location.getZ());
        this.plugin
                .getDataManager()
                .getConfig()
                .set("teams." + key + ".spawn.yaw", Float.valueOf(location.getYaw()));
        this.plugin
                .getDataManager()
                .getConfig()
                .set("teams." + key + ".spawn.pitch", Float.valueOf(location.getPitch()));
        this.plugin.getDataManager().save();
    }

    public Location getTeamSpawn(String team) {
        if (team == null) {
            return null;
        }
        String path = "teams." + this.normalize(team) + ".spawn";
        String world = this.plugin.getDataManager().getConfig().getString(path + ".world");
        if (world == null || Bukkit.getWorld(world) == null) {
            return null;
        }
        return new Location(
                Bukkit.getWorld(world),
                this.plugin.getDataManager().getConfig().getDouble(path + ".x"),
                this.plugin.getDataManager().getConfig().getDouble(path + ".y"),
                this.plugin.getDataManager().getConfig().getDouble(path + ".z"),
                (float) this.plugin.getDataManager().getConfig().getDouble(path + ".yaw"),
                (float) this.plugin.getDataManager().getConfig().getDouble(path + ".pitch"));
    }

    private String normalize(String input) {
        return input.toLowerCase(Locale.ROOT).trim();
    }

    private boolean isValidTeamName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        int minimum = Math.max(1, this.plugin.getConfig().getInt("teams.name.min-length", 3));
        int maximum =
                Math.max(
                        minimum,
                        Math.min(32, this.plugin.getConfig().getInt("teams.name.max-length", 16)));
        return trimmed.length() >= minimum
                && trimmed.length() <= maximum
                && trimmed.matches("[A-Za-z0-9_-]+");
    }

    private void clearInviteReferences(String team) {
        this.invites.entrySet().removeIf(entry -> (entry.getValue()).equalsIgnoreCase(team));
    }

    private void loadPersistentData() {
        ConfigurationSection teamsSection =
                this.plugin.getDataManager().getConfig().getConfigurationSection("teams");
        if (teamsSection == null) {
            return;
        }
        for (String team : teamsSection.getKeys(false)) {
            ChatColor color;
            String key = this.normalize(team);
            String displayName = teamsSection.getString(team + ".name", team);
            String owner = teamsSection.getString(team + ".owner");
            List<String> memberList = teamsSection.getStringList(team + ".members");
            if (owner != null && !owner.isBlank()) {
                try {
                    this.owners.put(key, UUID.fromString(owner));
                } catch (IllegalArgumentException exception) {
                    this.plugin
                            .getLogger()
                            .warning(
                                    "Ignoring invalid owner UUID for stored team "
                                            + team
                                            + ": "
                                            + owner);
                }
            }
            HashSet<UUID> loadedMembers = new HashSet<>();
            for (String member : memberList) {
                try {
                    loadedMembers.add(UUID.fromString(member));
                } catch (IllegalArgumentException exception) {
                    this.plugin
                            .getLogger()
                            .warning(
                                    "Ignoring invalid member UUID for stored team "
                                            + team
                                            + ": "
                                            + member);
                }
            }
            if (!loadedMembers.isEmpty()) {
                loadedMembers.removeIf(
                        member -> {
                            String previous = this.teamByMember.putIfAbsent(member, key);
                            if (previous == null) {
                                return false;
                            }
                            this.plugin
                                    .getLogger()
                                    .warning(
                                            "Player "
                                                    + member
                                                    + " is stored in multiple teams; keeping "
                                                    + previous
                                                    + '.');
                            return true;
                        });
            }
            if (!loadedMembers.isEmpty()) {
                this.members.put(key, loadedMembers);
                this.displayNames.put(key, displayName);
            }
            if ((color = this.parseGlowingColor(teamsSection.getString(team + ".glowing"))) == null
                    || !this.members.containsKey(key)) continue;
            this.glowingColors.put(key, color);
        }
    }

    private void removeTeam(String team) {
        String key = this.normalize(team);
        Set<UUID> teamMembers = this.members.getOrDefault(key, Set.of());
        for (UUID uuid : new HashSet<>(teamMembers)) {
            this.teamByMember.remove(uuid, key);
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            this.clearGlowing(player);
        }
        this.members.remove(key);
        this.owners.remove(key);
        this.displayNames.remove(key);
        this.clearInviteReferences(key);
        this.clearTeamGlowing(key);
        this.removeTeamData(key);
    }

    private Team getOrCreateScoreboardTeam(Scoreboard scoreboard, String team) {
        String name = this.scoreboardTeamName(team);
        Team scoreboardTeam = scoreboard.getTeam(name);
        if (scoreboardTeam == null) {
            scoreboardTeam = scoreboard.registerNewTeam(name);
        }
        return scoreboardTeam;
    }

    private void applyTeamGlowing(String team) {
        this.glowStateActive = true;
        for (Player player : this.getOnlineTeamPlayers(team)) {
            this.applyGlowing(player);
        }
    }

    private void addGlowingEntry(
            Scoreboard scoreboard, String team, ChatColor color, String entry) {
        this.removeEntryFromGlowTeams(scoreboard, entry);
        Team scoreboardTeam = this.getOrCreateScoreboardTeam(scoreboard, team);
        scoreboardTeam.setColor(color);
        scoreboardTeam.setPrefix(color.toString());
        if (!scoreboardTeam.hasEntry(entry)) {
            scoreboardTeam.addEntry(entry);
        }
    }

    private void syncMainScoreboardGlowing() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getScoreboard() == scoreboard) continue;
            viewer.setScoreboard(scoreboard);
        }
        for (Player target : Bukkit.getOnlinePlayers()) {
            String targetTeam = this.getTeamKey(target);
            ChatColor color = this.getOwnTeamGlowColor(target);
            if (color == null) {
                this.clearEntryFromLifeHeartsTeams(scoreboard, target.getName());
                target.setGlowing(false);
                continue;
            }
            this.addGlowingEntry(scoreboard, targetTeam, color, target.getName());
            target.setGlowing(true);
        }
    }

    private void syncGlowingForViewer(Player viewer) {
        Scoreboard scoreboard = this.getGlowScoreboard(viewer);
        for (Player target : Bukkit.getOnlinePlayers()) {
            ChatColor color = this.getViewerGlowColor(viewer, target);
            if (color == null) {
                this.clearEntryFromLifeHeartsTeams(scoreboard, target.getName());
                if (this.shouldGlowForAnyViewer(target)) continue;
                target.setGlowing(false);
                continue;
            }
            this.addGlowingEntry(scoreboard, this.getTeamKey(target), color, target.getName());
            target.setGlowing(true);
        }
    }

    private void syncGlowingForTarget(Player target) {
        ChatColor mainColor;
        if (!this.isGlowingEnabled()) {
            this.clearGlowing(target);
            return;
        }
        boolean shouldGlow = false;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            ChatColor color = this.getViewerGlowColor(viewer, target);
            if (color == null) {
                this.clearEntryFromLifeHeartsTeams(
                        this.getGlowScoreboard(viewer), target.getName());
                continue;
            }
            this.addGlowingEntry(
                    this.getGlowScoreboard(viewer),
                    this.getTeamKey(target),
                    color,
                    target.getName());
            shouldGlow = true;
        }
        ChatColor chatColor =
                mainColor =
                        this.shouldUseMainScoreboardMode()
                                ? this.getOwnTeamGlowColor(target)
                                : null;
        if (mainColor != null) {
            this.addGlowingEntry(
                    Bukkit.getScoreboardManager().getMainScoreboard(),
                    this.getTeamKey(target),
                    mainColor,
                    target.getName());
            shouldGlow = true;
        }
        target.setGlowing(shouldGlow);
    }

    private ChatColor getViewerGlowColor(Player viewer, Player target) {
        if (!this.isGlowingEnabled()) {
            return null;
        }
        String viewerTeam = this.getTeamKey(viewer);
        String targetTeam = this.getTeamKey(target);
        if (this.isEnemyGlowingEnabled()
                && viewerTeam != null
                && targetTeam != null
                && !viewerTeam.equalsIgnoreCase(targetTeam)) {
            ChatColor enemyColor =
                    this.parseGlowingColor(
                            this.plugin
                                    .getConfig()
                                    .getString("teams.enemy-glowing.color", "dark_red"));
            return enemyColor == null ? ChatColor.DARK_RED : enemyColor;
        }
        return this.getOwnTeamGlowColor(target);
    }

    private ChatColor getOwnTeamGlowColor(Player target) {
        String team = this.getTeamKey(target);
        return team == null ? null : this.glowingColors.get(this.normalize(team));
    }

    private boolean shouldGlowForAnyViewer(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (this.getViewerGlowColor(viewer, target) == null) continue;
            return true;
        }
        return false;
    }

    private Scoreboard getGlowScoreboard(Player viewer) {
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (this.shouldUseMainScoreboardMode()) {
            if (viewer.getScoreboard() != mainScoreboard) {
                viewer.setScoreboard(mainScoreboard);
            }
            return mainScoreboard;
        }
        if (this.isEnemyGlowingEnabled()) {
            Scoreboard scoreboard =
                    this.viewerGlowScoreboards.computeIfAbsent(
                            viewer.getUniqueId(),
                            ignored -> Bukkit.getScoreboardManager().getNewScoreboard());
            if (viewer.getScoreboard() != scoreboard) {
                viewer.setScoreboard(scoreboard);
            }
            return scoreboard;
        }
        return viewer.getScoreboard();
    }

    private void clearGlowing(Player player) {
        String entry = player.getName();
        this.clearGlowingFromScoreboard(Bukkit.getScoreboardManager().getMainScoreboard(), entry);
        for (Scoreboard scoreboard : this.viewerGlowScoreboards.values()) {
            this.clearGlowingFromScoreboard(scoreboard, entry);
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            this.clearGlowingFromScoreboard(viewer.getScoreboard(), entry);
        }
        player.setGlowing(false);
    }

    private void clearGlowingFromScoreboard(Scoreboard scoreboard, String entry) {
        this.clearEntryFromLifeHeartsTeams(scoreboard, entry);
    }

    private void clearTeamGlowing(String team) {
        this.clearTeamGlowingFromScoreboard(
                Bukkit.getScoreboardManager().getMainScoreboard(), team);
        for (Scoreboard scoreboard : this.viewerGlowScoreboards.values()) {
            this.clearTeamGlowingFromScoreboard(scoreboard, team);
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            this.clearTeamGlowingFromScoreboard(viewer.getScoreboard(), team);
        }
        this.glowingColors.remove(this.normalize(team));
    }

    private boolean isGlowingEnabled() {
        return this.plugin.getConfig().getBoolean("teams.glowing.enabled", true);
    }

    private boolean isEnemyGlowingEnabled() {
        return this.plugin.getConfig().getBoolean("teams.enemy-glowing.enabled", false);
    }

    private boolean shouldUseMainScoreboardMode() {
        return this.plugin.getConfig().getBoolean("teams.glowing.force-main-scoreboard", false)
                && !this.isEnemyGlowingEnabled();
    }

    private void clearAllLifeHeartsGlowing() {
        this.clearLifeHeartsTeams(Bukkit.getScoreboardManager().getMainScoreboard());
        for (Scoreboard scoreboard : this.viewerGlowScoreboards.values()) {
            this.clearLifeHeartsTeams(scoreboard);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.clearLifeHeartsTeams(player.getScoreboard());
            if (!player.isGlowing()) continue;
            player.setGlowing(false);
        }
        this.releaseViewerGlowScoreboards();
        this.glowStateActive = false;
    }

    private void clearLifeHeartsTeams(Scoreboard scoreboard) {
        for (Team scoreboardTeam : new HashSet<>(scoreboard.getTeams())) {
            if (!scoreboardTeam.getName().startsWith("lhpg")) continue;
            scoreboardTeam.unregister();
        }
    }

    private void cleanupStaleGlowScoreboards() {
        HashSet<UUID> online = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            online.add(player.getUniqueId());
        }
        this.viewerGlowScoreboards.keySet().removeIf(uuid -> !online.contains(uuid));
    }

    private void releaseViewerGlowScoreboards() {
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard glowScoreboard = this.viewerGlowScoreboards.get(player.getUniqueId());
            if (glowScoreboard == null || player.getScoreboard() != glowScoreboard) continue;
            player.setScoreboard(mainScoreboard);
        }
        this.viewerGlowScoreboards.clear();
    }

    private void clearTeamGlowingFromScoreboard(Scoreboard scoreboard, String team) {
        Team scoreboardTeam = scoreboard.getTeam(this.scoreboardTeamName(team));
        if (scoreboardTeam != null) {
            for (String entry : new HashSet<>(scoreboardTeam.getEntries())) {
                scoreboardTeam.removeEntry(entry);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry);
                if (!offlinePlayer.isOnline() || offlinePlayer.getPlayer() == null) continue;
                offlinePlayer.getPlayer().setGlowing(false);
            }
            scoreboardTeam.unregister();
        }
    }

    private void removeTeamSpawn(String team) {
        this.plugin
                .getDataManager()
                .getConfig()
                .set("teams." + this.normalize(team) + ".spawn", null);
        this.plugin.getDataManager().save();
    }

    private void removeTeamData(String team) {
        this.plugin.getDataManager().getConfig().set("teams." + this.normalize(team), null);
        this.plugin.getDataManager().save();
    }

    private void saveTeam(String team) {
        String key = this.normalize(team);
        Set<UUID> teamMembers = this.members.get(key);
        if (teamMembers == null || teamMembers.isEmpty()) {
            return;
        }
        this.plugin
                .getDataManager()
                .getConfig()
                .set("teams." + key + ".name", this.getDisplayName(key));
        UUID owner = this.owners.get(key);
        if (owner != null) {
            this.plugin
                    .getDataManager()
                    .getConfig()
                    .set("teams." + key + ".owner", owner.toString());
        }
        ArrayList<String> storedMembers = new ArrayList<>();
        for (UUID uuid : teamMembers) {
            storedMembers.add(uuid.toString());
        }
        this.plugin.getDataManager().getConfig().set("teams." + key + ".members", storedMembers);
        ChatColor color = this.glowingColors.get(key);
        if (color != null) {
            this.plugin.getDataManager().getConfig().set("teams." + key + ".glowing", color.name());
        }
        this.plugin.getDataManager().save();
    }

    private String getDisplayName(String team) {
        return this.displayNames.getOrDefault(this.normalize(team), team);
    }

    private void clearEntryFromLifeHeartsTeams(Scoreboard scoreboard, String entry) {
        for (Team scoreboardTeam : scoreboard.getTeams()) {
            if (!scoreboardTeam.getName().startsWith("lhpg") || !scoreboardTeam.hasEntry(entry))
                continue;
            scoreboardTeam.removeEntry(entry);
        }
    }

    private void removeEntryFromGlowTeams(Scoreboard scoreboard, String entry) {
        for (Team scoreboardTeam : scoreboard.getTeams()) {
            if (!scoreboardTeam.getName().startsWith("lhpg") || !scoreboardTeam.hasEntry(entry))
                continue;
            scoreboardTeam.removeEntry(entry);
        }
    }

    private String scoreboardTeamName(String team) {
        return "lhpg" + Integer.toHexString(this.normalize(team).hashCode());
    }
}
