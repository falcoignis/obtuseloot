package obtuseloot.equipment;

import obtuseloot.artifacts.Artifact;
import org.bukkit.inventory.EquipmentSlot;
import java.util.*;

/** Main-thread ownership of equipped identities. Inventory items never enter this set. */
public final class EquipmentBindings {
    public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private final Map<UUID, Map<EquipmentSlot, EquippedArtifact>> equipped = new HashMap<>();
    private final Map<String, UUID> claims = new HashMap<>();
    public Map<EquipmentSlot, EquippedArtifact> get(UUID player) { return equipped.getOrDefault(player, Map.of()); }
    public Map<EquipmentSlot, EquippedArtifact> reconcile(UUID player, Map<EquipmentSlot, Artifact> candidates) {
        var previous = get(player);
        var current = new EnumMap<EquipmentSlot, EquippedArtifact>(EquipmentSlot.class);
        var keys = new HashSet<String>();
        for (EquipmentSlot slot : SLOTS) {
            Artifact artifact = candidates.get(slot);
            if (artifact == null || artifact.getArtifactSeed() == 0) continue;
            String key = artifact.getArtifactStorageKey();
            UUID claimed = claims.get(key);
            if (claimed != null && !claimed.equals(player) || !keys.add(key)) continue;
            current.put(slot, new EquippedArtifact(slot, artifact));
        }
        for (var old : previous.values()) if (!keys.contains(old.key())) claims.remove(old.key(), player);
        for (var binding : current.values()) claims.put(binding.key(), player);
        Map<EquipmentSlot, EquippedArtifact> snapshot = Collections.unmodifiableMap(current);
        if (current.isEmpty()) equipped.remove(player); else equipped.put(player, snapshot);
        return snapshot;
    }
    public void clear(UUID player) { reconcile(player, Map.of()); }
}
