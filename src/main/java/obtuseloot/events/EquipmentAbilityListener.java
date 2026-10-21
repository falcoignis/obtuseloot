package obtuseloot.events;

import obtuseloot.ObtuseLoot;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Event reconciliation runs after vanilla inventory changes; the shared tick loop is a safety net. */
public final class EquipmentAbilityListener implements Listener {
    private final Set<UUID> pending = new HashSet<>();
    private void later(Player player) {
        if (!pending.add(player.getUniqueId())) return;
        var plugin = ObtuseLoot.get();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pending.remove(player.getUniqueId());
            if (player.isOnline()) plugin.getEquipmentAbilities().reconcile(player);
        });
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void inventory(InventoryClickEvent event) { if (event.getWhoClicked() instanceof Player player) later(player); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void drag(InventoryDragEvent event) { if (event.getWhoClicked() instanceof Player player) later(player); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void held(PlayerItemHeldEvent event) { later(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void swap(PlayerSwapHandItemsEvent event) { later(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void drop(PlayerDropItemEvent event) { later(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void broken(PlayerItemBreakEvent event) { later(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void interact(PlayerInteractEvent event) { later(event.getPlayer()); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void death(PlayerDeathEvent event) { ObtuseLoot.get().getEquipmentAbilities().suspend(event.getEntity()); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void respawn(PlayerRespawnEvent event) {
        var plugin = ObtuseLoot.get();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) plugin.getEquipmentAbilities().resume(event.getPlayer());
        });
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void world(PlayerChangedWorldEvent event) {
        ObtuseLoot.get().getEquipmentAbilities().suspend(event.getPlayer());
        var plugin = ObtuseLoot.get();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) plugin.getEquipmentAbilities().resume(event.getPlayer());
        });
    }
}
