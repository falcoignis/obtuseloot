package obtuseloot.equipment;

import obtuseloot.artifacts.Artifact;
import org.bukkit.inventory.EquipmentSlot;

/** A binding captures identity so a later seed replacement invalidates deferred work. */
public record EquippedArtifact(EquipmentSlot slot, Artifact artifact, String key, long seed) {
    public EquippedArtifact(EquipmentSlot slot, Artifact artifact) {
        this(slot, artifact, artifact.getArtifactStorageKey(), artifact.getArtifactSeed());
    }
    public boolean sameIdentity(EquippedArtifact other) {
        return other != null && key.equals(other.key) && seed == other.seed;
    }
}
