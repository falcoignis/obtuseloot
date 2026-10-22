package obtuseloot.events;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinLoadListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        completeLoad(event.getPlayer());
    }

    /**
     * Loads the player's state and wires their abilities. If storage is unreadable this does nothing;
     * it runs again when the artifact and reputation managers report a recovered load.
     */
    public static void completeLoad(Player player) {
        ObtuseLoot plugin = ObtuseLoot.get();
        var playerId = player.getUniqueId();

        Artifact artifact = plugin.getArtifactItemStorage().activateHeld(player);
        ArtifactReputation reputation = artifact == null ? null : artifact.getItemReputation();
        if (artifact == null || reputation == null) {
            return;
        }
        plugin.getArtifactManager().markDirty(artifact);
        plugin.getItemAbilityManager().rebuildSubscriptions(playerId, artifact, reputation, "player-join-load");
        plugin.getLoreEngine().refreshLore(player, artifact, reputation);
    }
}
