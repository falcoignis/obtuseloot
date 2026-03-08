package obtuseloot.obtuseengine;

import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.lore.LoreEngine;
import obtuseloot.reputation.ReputationManager;

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
