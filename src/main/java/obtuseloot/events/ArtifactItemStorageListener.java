package obtuseloot.events;

import obtuseloot.ObtuseLoot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class ArtifactItemStorageListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int migrated = ObtuseLoot.get().getArtifactItemStorage().migrateInventory(player);
        if (migrated > 0) {
            ObtuseLoot.get().getLogger().info("[ArtifactStorage] Join migration converted " + migrated + " items for " + player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        ObtuseLoot.get().getArtifactItemStorage().migrateLegacyIfPresent(event.getPlayer(), item);
    }
}
