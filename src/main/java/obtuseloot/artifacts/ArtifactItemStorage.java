package obtuseloot.artifacts;

import obtuseloot.ObtuseLoot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.UUID;

public final class ArtifactItemStorage {
    private final ObtuseLoot plugin;
    private final ArtifactPdcKeys keys;
    private final ArtifactStateCodec codec;

    public ArtifactItemStorage(ObtuseLoot plugin) {
        this.plugin = plugin;
        this.keys = new ArtifactPdcKeys(plugin);
        this.codec = new ArtifactStateCodec(keys);
    }

    public boolean isArtifactItem(ItemStack item) {
        return readStorageKey(item) != null;
    }

    public String readStorageKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return codec.readStorageKey(meta);
    }

    public void stampMinimalIdentity(ItemStack item, Artifact artifact) {
        if (item == null || artifact == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        codec.write(meta, artifact);
        item.setItemMeta(meta);
    }

    public Artifact resolve(ItemStack item, UUID fallbackOwner) {
        String storageKey = readStorageKey(item);
        if (storageKey != null) {
            if (!ArtifactStorageKeys.isItem(storageKey)) return null;
            Artifact resolved = plugin.getArtifactManager().resolveByStorageKey(storageKey);
            return resolved; // a missing tracked identity must never fall back to another artifact
        }
        if (fallbackOwner != null) {
            return plugin.getArtifactManager().tryGet(fallbackOwner);
        }
        return null;
    }

    /** Bind the held physical item before gameplay. Switching never transfers its history to another item. */
    public Artifact activateHeld(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isArtifactItem(held)) {
            Artifact previous = plugin.getArtifactManager().getLoadedArtifacts().get(player.getUniqueId());
            if (previous != null && ArtifactStorageKeys.isItem(previous.getArtifactStorageKey())) {
                plugin.getEngineScheduler().flushEvolution(player.getUniqueId());
                plugin.getArtifactManager().unload(player.getUniqueId());
                plugin.getReputationManager().unbindItem(player.getUniqueId());
                plugin.getItemAbilityManager().clearSubscriptions(player.getUniqueId());
                plugin.getCombatContextManager().remove(player.getUniqueId());
            }
            return null;
        }
        try {
            Artifact artifact = resolve(held, null);
            if (artifact == null) return null;
            if (!ArtifactStorageKeys.isItem(artifact.getArtifactStorageKey())) return artifact;
            UUID holder = player.getUniqueId();
            Artifact current = plugin.getArtifactManager().getLoadedArtifacts().get(holder);
            if (current != artifact || !holder.equals(artifact.getOwnerId())
                    || plugin.getReputationManager().getLoadedReputations().get(holder) != artifact.getItemReputation()) {
                UUID previousHolder = artifact.getOwnerId();
                plugin.getEngineScheduler().flushEvolution(holder);
                if (!previousHolder.equals(holder)
                        && plugin.getArtifactManager().getLoadedArtifacts().get(previousHolder) == artifact) {
                    plugin.getEngineScheduler().flushEvolution(previousHolder);
                    plugin.getReputationManager().unbindItem(previousHolder);
                    plugin.getItemAbilityManager().clearSubscriptions(previousHolder);
                    plugin.getCombatContextManager().remove(previousHolder);
                }
                plugin.getArtifactManager().activateItem(holder, artifact);
                plugin.getReputationManager().bindItem(holder, artifact);
                if (current != artifact) plugin.getCombatContextManager().remove(holder);
                plugin.getItemAbilityManager().clearSubscriptions(holder);
                plugin.getItemAbilityManager().rebuildSubscriptions(holder, artifact, artifact.getItemReputation(), "physical-item-activation");
                stampMinimalIdentity(held, artifact);
            }
            return artifact;
        } catch (obtuseloot.persistence.PlayerStateLoadException exception) {
            return null; // the shared load guard reports failures and applies retry backoff
        } catch (RuntimeException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Refusing held artifact for " + player.getUniqueId(), exception);
            return null;
        }
    }

    /** No player data exists to migrate; old player-scoped tags are never restamped onto physical items. */
    public boolean migrateLegacyIfPresent(Player player, ItemStack item) { return false; }
    public int migrateInventory(Player player) { return 0; }

    public String describeItemStorage(ItemStack item) {
        if (item == null) {
            return "none";
        }
        String key = readStorageKey(item);
        if (key == null) {
            return "untracked";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return "untracked";
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer version = pdc.get(keys.artifactVersionKey(), PersistentDataType.INTEGER);
        String owner = pdc.get(keys.artifactOwnerKey(), PersistentDataType.STRING);
        return "key=" + key + ", owner=" + Objects.toString(owner, "unknown") + ", version=" + Objects.toString(version, "?");
    }
}
