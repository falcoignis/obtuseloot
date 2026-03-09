package obtuseloot.combat;

import obtuseloot.obtuseengine.ArtifactProcessor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatCore implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onCombatHit(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event);
        if (attacker == null) {
            return;
        }
        ArtifactProcessor.processCombat(attacker, event);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
