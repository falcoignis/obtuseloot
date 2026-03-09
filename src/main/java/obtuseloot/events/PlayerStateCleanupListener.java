package obtuseloot.events;

import obtuseloot.ObtuseLoot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerStateCleanupListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ObtuseLoot plugin = ObtuseLoot.get();
        var playerId = event.getPlayer().getUniqueId();

        plugin.getArtifactManager().unload(playerId);
        plugin.getReputationManager().unload(playerId);
        plugin.getCombatContextManager().remove(playerId);
        plugin.getLoreEngine().removePlayer(playerId);
    }
}
