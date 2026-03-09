package obtuseloot.obtuseengine;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.reputation.ReputationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerStateCleanupListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        ArtifactManager.remove(playerId);
        ReputationManager.remove(playerId);
        ObtuseLoot.get().getCombatContextManager().remove(playerId);
    }
}
