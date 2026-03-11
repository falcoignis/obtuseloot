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
            Artifact resolved = plugin.getArtifactManager().resolveByStorageKey(storageKey);
            if (resolved != null) {
                return resolved;
            }
        }
        if (fallbackOwner != null) {
            return plugin.getArtifactManager().getOrCreate(fallbackOwner);
        }
        return null;
    }

    public boolean migrateLegacyIfPresent(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.legacyBlobKey(), PersistentDataType.STRING) && !pdc.has(keys.artifactStorageKey(), PersistentDataType.STRING)) {
            return false;
        }

        Artifact artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
        stampMinimalIdentity(item, artifact);
        plugin.getArtifactManager().save(player.getUniqueId());
        plugin.getLogger().info("[ArtifactStorage] Migrated legacy artifact item for " + player.getUniqueId()
                + " to minimal identity key=" + artifact.getArtifactStorageKey());
        return true;
    }

    public int migrateInventory(Player player) {
        int migrated = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && migrateLegacyIfPresent(player, item)) {
                migrated++;
            }
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && migrateLegacyIfPresent(player, item)) {
                migrated++;
            }
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && migrateLegacyIfPresent(player, offHand)) {
            migrated++;
        }
        return migrated;
    }

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
