package obtuseloot.events;

import obtuseloot.ObtuseLoot;
import obtuseloot.combat.CombatContext;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class ReputationFeedListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event);
        if (attacker == null) {
            return;
        }

        CombatContext context = ObtuseLoot.get().getCombatContextManager().get(attacker.getUniqueId());
        context.markCombat();
        if (event.getEntity() instanceof LivingEntity target) {
            context.addTarget(target.getUniqueId());
        }
        context.setLastWeaponCategory(classifyWeapon(attacker));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }

        CombatContext context = ObtuseLoot.get().getCombatContextManager().get(event.getPlayer().getUniqueId());
        long now = System.currentTimeMillis();
        if (!context.isInCombatWindow(now, RuntimeSettings.get().combatWindowMs())) {
            return;
        }

        double distance = event.getFrom().distance(event.getTo());
        if (distance > 0) {
            context.addMovement(distance);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        CombatContext context = ObtuseLoot.get().getCombatContextManager().get(killer.getUniqueId());
        ArtifactReputation rep = ObtuseLoot.get().getReputationManager().get(killer.getUniqueId());

        long now = System.currentTimeMillis();
        context.addKillTimestamp(now);

        if (isBoss(event.getEntity())) {
            rep.recordBossKill();
        }

        int chain = context.countKillsWithinWindow(now, RuntimeSettings.get().killChainWindowMs());
        if (chain >= 2) {
            rep.recordKillChain(chain);
            rep.recordChaos();
        }

        if (context.getRecentTargets().size() >= RuntimeSettings.get().multiTargetChaosThreshold()) {
            rep.recordChaos();
        }

        if (killer.getHealth() <= RuntimeSettings.get().lowHealthThreshold() || context.isLowHealthFlag()) {
            rep.recordSurvival();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ArtifactReputation rep = ObtuseLoot.get().getReputationManager().get(player.getUniqueId());
        CombatContext context = ObtuseLoot.get().getCombatContextManager().get(player.getUniqueId());

        rep.resetOnDeath();
        context.resetTransient();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        CombatContext context = ObtuseLoot.get().getCombatContextManager().get(player.getUniqueId());
        double postDamageHealth = Math.max(0.0D, player.getHealth() - event.getFinalDamage());
        context.setLastKnownHealth(postDamageHealth);

        if (postDamageHealth <= RuntimeSettings.get().lowHealthThreshold()) {
            context.setLowHealthFlag(true);
            context.setLowHealthEnteredAt(System.currentTimeMillis());
        }
    }

    public Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    public boolean isBoss(Entity entity) {
        return RuntimeSettings.get().bossTypes().contains(entity.getType().name());
    }

    public boolean isProjectilePrecisionHit(EntityDamageByEntityEvent event) {
        return event.getDamager() instanceof Projectile && event.getFinalDamage() >= RuntimeSettings.get().precisionThresholdDamage();
    }

    public String classifyWeapon(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            return "unarmed";
        }
        String name = hand.getType().name();
        if (name.contains("BOW") || name.contains("CROSSBOW") || name.contains("TRIDENT")) {
            return "ranged";
        }
        if (name.contains("SWORD") || name.contains("AXE")) {
            return "melee";
        }
        return "other";
    }
}
