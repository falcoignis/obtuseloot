package obtuseloot.events;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinLoadListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ObtuseLoot plugin = ObtuseLoot.get();
        var playerId = event.getPlayer().getUniqueId();

        Artifact artifact = plugin.getArtifactManager().getOrCreate(playerId);
        ArtifactReputation reputation = plugin.getReputationManager().get(playerId);
        plugin.getItemAbilityManager().rebuildSubscriptions(playerId, artifact, reputation, "player-join-load");
        plugin.getLoreEngine().refreshLore(event.getPlayer(), artifact, reputation);
    }
}
