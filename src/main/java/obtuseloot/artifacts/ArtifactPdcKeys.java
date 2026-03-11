package obtuseloot.artifacts;

import obtuseloot.ObtuseLoot;
import org.bukkit.NamespacedKey;

public final class ArtifactPdcKeys {
    private final NamespacedKey artifactStorageKey;
    private final NamespacedKey artifactOwnerKey;
    private final NamespacedKey artifactVersionKey;
    private final NamespacedKey artifactSeedKey;
    private final NamespacedKey mechanicFingerprintKey;
    private final NamespacedKey memoryPressureKey;
    private final NamespacedKey legacyBlobKey;

    public ArtifactPdcKeys(ObtuseLoot plugin) {
        this.artifactStorageKey = new NamespacedKey(plugin, "artifact-storage-key");
        this.artifactOwnerKey = new NamespacedKey(plugin, "artifact-owner-id");
        this.artifactVersionKey = new NamespacedKey(plugin, "artifact-storage-version");
        this.artifactSeedKey = new NamespacedKey(plugin, "artifact-seed");
        this.mechanicFingerprintKey = new NamespacedKey(plugin, "mechanic-fingerprint");
        this.memoryPressureKey = new NamespacedKey(plugin, "memory-pressure");
        this.legacyBlobKey = new NamespacedKey(plugin, "artifact-state");
    }

    public NamespacedKey artifactStorageKey() { return artifactStorageKey; }
    public NamespacedKey artifactOwnerKey() { return artifactOwnerKey; }
    public NamespacedKey artifactVersionKey() { return artifactVersionKey; }
    public NamespacedKey artifactSeedKey() { return artifactSeedKey; }
    public NamespacedKey mechanicFingerprintKey() { return mechanicFingerprintKey; }
    public NamespacedKey memoryPressureKey() { return memoryPressureKey; }
    public NamespacedKey legacyBlobKey() { return legacyBlobKey; }
}
