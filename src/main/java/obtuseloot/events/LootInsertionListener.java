package obtuseloot.events;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.*;
import obtuseloot.ecology.WorldPosition;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/** Identity only: it does not add, replace or duplicate any generated item. */
public final class LootInsertionListener implements Listener {
    private static final UUID UNCLAIMED = new UUID(0, 0);
    private final obtuseloot.ecology.LocalEcosystem ecosystem;
    private final java.util.function.Predicate<ItemStack> tracked;
    private final java.util.function.BiConsumer<ItemStack, Artifact> stamp;
    private final java.util.logging.Logger logger;
    public LootInsertionListener() {
        this(ObtuseLoot.get().getLocalEcosystem(), ObtuseLoot.get().getArtifactItemStorage()::isArtifactItem,
                ObtuseLoot.get().getArtifactItemStorage()::stampMinimalIdentity, ObtuseLoot.get().getLogger());
    }
    public LootInsertionListener(obtuseloot.ecology.LocalEcosystem ecosystem, java.util.function.Predicate<ItemStack> tracked,
                                 java.util.function.BiConsumer<ItemStack, Artifact> stamp, java.util.logging.Logger logger) {
        this.ecosystem = ecosystem; this.tracked = tracked; this.stamp = stamp; this.logger = logger;
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onLootGenerate(LootGenerateEvent event) {
        if (event.isCancelled()) return;
        UUID holder = event.getEntity() instanceof Player player ? player.getUniqueId() : UNCLAIMED;
        var position = WorldPosition.from(event.getLootContext().getLocation());
        for (ItemStack item : event.getLoot()) stampIfEligible(item, holder, position);
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEnchant(EnchantItemEvent event) {
        if (event.isCancelled()) return;
        stampIfEligible(event.getItem(), event.getEnchanter().getUniqueId(), WorldPosition.from(event.getEnchantBlock()));
    }
    private void stampIfEligible(ItemStack item, UUID holder, WorldPosition position) {
        if (item == null || item.getAmount() != 1 || tracked.test(item)) return;
        String material = item.getType().name().toLowerCase(Locale.ROOT);
        if (!EquipmentArchetype.isEquipment(material)) return;
        try {
            var artifact = ecosystem.createEquipment(holder, EquipmentArchetype.fromId(material), position);
            stamp.accept(item, artifact);
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, "Artifact inheritance failed; generated item left unstamped", exception);
        }
    }
}
