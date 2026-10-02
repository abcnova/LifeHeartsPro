package de.nova.lifehearts.listener;

import de.nova.lifehearts.LifeHeartsPlugin;
import de.nova.lifehearts.manager.TeamManager;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/** Applies friendly-fire rules to melee, projectiles and player-created area effect clouds. */
public final class TeamDamageListener implements Listener {
    private final TeamManager teamManager;
    private final LifeHeartsPlugin plugin;

    public TeamDamageListener(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null
                || attacker.getUniqueId().equals(victim.getUniqueId())
                || teamManager.canDamage(attacker, victim)) {
            return;
        }
        event.setCancelled(true);
        plugin.getLocaleManager().send(attacker, "team-friendly-fire-disabled");
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        ProjectileSource source = null;
        if (damager instanceof Projectile projectile) {
            source = projectile.getShooter();
        } else if (damager instanceof AreaEffectCloud cloud) {
            source = cloud.getSource();
        }
        return source instanceof Player player ? player : null;
    }
}
