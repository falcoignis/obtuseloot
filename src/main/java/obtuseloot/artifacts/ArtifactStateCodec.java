package obtuseloot.artifacts;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class ArtifactStateCodec {
    private static final int STORAGE_VERSION = 3;

    private final ArtifactPdcKeys keys;

    public ArtifactStateCodec(ArtifactPdcKeys keys) {
        this.keys = keys;
    }

    public void write(ItemMeta meta, Artifact artifact) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keys.artifactStorageKey(), PersistentDataType.STRING, artifact.getArtifactStorageKey());
        pdc.set(keys.artifactOwnerKey(), PersistentDataType.STRING, artifact.getOwnerId().toString());
        pdc.set(keys.artifactVersionKey(), PersistentDataType.INTEGER, STORAGE_VERSION);
        pdc.set(keys.artifactSeedKey(), PersistentDataType.LONG, artifact.getArtifactSeed());
        pdc.set(keys.mechanicFingerprintKey(), PersistentDataType.STRING, artifact.getLastMechanicProfile());
        pdc.set(keys.memoryPressureKey(), PersistentDataType.INTEGER, artifact.getMemory().pressure());
        pdc.remove(keys.legacyBlobKey());
    }

    public String readStorageKey(ItemMeta meta) {
        return meta.getPersistentDataContainer().get(keys.artifactStorageKey(), PersistentDataType.STRING);
    }
}
