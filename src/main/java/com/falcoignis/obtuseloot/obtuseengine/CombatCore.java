package com.falcoignis.obtuseloot.obtuseengine;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatCore implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onCombatHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        ArtifactProcessor.processCombat(player, event);
    }
}
