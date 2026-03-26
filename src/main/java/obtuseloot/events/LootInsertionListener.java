package obtuseloot.events;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Stamps generated loot/enchanted items with minimal artifact identity so those
 * items can resolve back to the owning player's artifact profile.
 */
public final class LootInsertionListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onLootGenerate(LootGenerateEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        List<ItemStack> generatedLoot = event.getLoot();
        if (generatedLoot == null || generatedLoot.isEmpty()) {
            return;
        }

        Artifact artifact = ObtuseLoot.get().getArtifactManager().getOrCreate(player.getUniqueId());
        for (ItemStack item : generatedLoot) {
            stampIfEligible(item, artifact);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEnchant(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        Player player = event.getEnchanter();
        Artifact artifact = ObtuseLoot.get().getArtifactManager().getOrCreate(player.getUniqueId());
        stampIfEligible(item, artifact);
    }

    private void stampIfEligible(ItemStack item, Artifact artifact) {
        if (item == null || item.getType() == Material.AIR || artifact == null) {
            return;
        }
        ObtuseLoot.get().getArtifactItemStorage().stampMinimalIdentity(item, artifact);
    }
}
