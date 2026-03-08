package com.obtuseloot.obtuseengine;

import com.obtuseloot.artifacts.ArtifactManager;
import com.obtuseloot.lore.LoreEngine;
import com.obtuseloot.reputation.ReputationManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Clears in-memory per-player state for offline players to avoid stale growth on long-lived servers.
 */
public final class PlayerStateCleanupListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        ArtifactManager.remove(playerId);
        ReputationManager.remove(playerId);
        LoreEngine.removePlayer(playerId);
    }
}
