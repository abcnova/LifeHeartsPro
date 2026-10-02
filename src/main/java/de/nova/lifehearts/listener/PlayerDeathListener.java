package de.nova.lifehearts.listener;

import de.nova.lifehearts.LifeHeartsPlugin;
import de.nova.lifehearts.manager.LifeManager;
import de.nova.lifehearts.manager.LocaleManager;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

public class PlayerDeathListener implements Listener {
    private final LifeHeartsPlugin plugin;
    private final LifeManager lifeManager;
    private final LocaleManager locale;

    public PlayerDeathListener(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
        this.lifeManager = plugin.getLifeManager();
        this.locale = plugin.getLocaleManager();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (this.plugin.getConfig().getBoolean("lives.only-lose-life-when-killed-by-player", true)
                && !this.wasKilledDirectlyByPlayer(player)) {
            this.locale.send(
                    player,
                    "lives-message",
                    "%lives%",
                    String.valueOf(this.lifeManager.getLives(player)));
            return;
        }
        this.lifeManager.handleDeath(player);
        this.locale.send(
                player,
                "lives-message",
                "%lives%",
                String.valueOf(this.lifeManager.getLives(player)));
        if (this.lifeManager.isDead(player)) {
            this.lifeManager.handleElimination(player);
        }
    }

    private boolean wasKilledDirectlyByPlayer(Player player) {
        EntityDamageEvent entityDamageEvent = player.getLastDamageCause();
        if (!(entityDamageEvent instanceof EntityDamageByEntityEvent)) {
            return false;
        }
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) entityDamageEvent;
        Player attacker = this.resolvePlayerAttacker(event.getDamager());
        return attacker != null && !attacker.getUniqueId().equals(player.getUniqueId());
    }

    private Player resolvePlayerAttacker(Entity damager) {
        if (damager instanceof Player) {
            Player player = (Player) damager;
            return player;
        }
        if (damager instanceof Projectile) {
            Player player;
            Projectile projectile = (Projectile) damager;
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Player ? (player = (Player) shooter) : null;
        }
        if (damager instanceof AreaEffectCloud) {
            Player player;
            AreaEffectCloud cloud = (AreaEffectCloud) damager;
            ProjectileSource source = cloud.getSource();
            return source instanceof Player ? (player = (Player) source) : null;
        }
        return null;
    }
}
