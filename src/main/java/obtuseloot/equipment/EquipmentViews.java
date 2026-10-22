package obtuseloot.equipment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.inventory.EquipmentSlot;

/** Immutable, read-only publication boundary for asynchronous PlaceholderAPI consumers. */
public final class EquipmentViews {
    private volatile Map<String, String> integrations = Map.of();
    public void integrations(Map<String, String> status) { integrations = Map.copyOf(status); }
    private final Map<UUID, Map<String, Map<String, String>>> snapshots = new ConcurrentHashMap<>();
    private static final Map<String, EquipmentSlot> SLOTS = Map.of("mainhand", EquipmentSlot.HAND,
            "offhand", EquipmentSlot.OFF_HAND, "head", EquipmentSlot.HEAD, "chest", EquipmentSlot.CHEST,
            "legs", EquipmentSlot.LEGS, "feet", EquipmentSlot.FEET);
    public static String slotId(EquipmentSlot slot) {
        return SLOTS.entrySet().stream().filter(e -> e.getValue() == slot).findFirst().orElseThrow().getKey();
    }
    public void publish(UUID player, Map<EquipmentSlot, EquippedArtifact> bindings,
                        Map<EquipmentSlot, List<String>> abilities, Set<String> passive) {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (var entry : bindings.entrySet()) {
            var binding = entry.getValue(); var artifact = binding.artifact();
            var values = new HashMap<String, String>();
            values.put("present", "true"); values.put("name", Objects.toString(artifact.getDisplayName(), ""));
            values.put("key", binding.key()); values.put("seed", Long.toString(binding.seed()));
            values.put("lineage", Objects.toString(artifact.getLatentLineage(), ""));
            values.put("generation", Integer.toString(artifact.getInheritanceGeneration()));
            values.put("evolution", Objects.toString(artifact.getEvolutionPath(), ""));
            values.put("reputation", Integer.toString(artifact.getItemReputation().getTotalScore()));
            values.put("abilities", String.join(", ", abilities.getOrDefault(entry.getKey(), List.of())));
            values.put("passives", passive.stream().filter(k -> k.startsWith(binding.key() + "@"))
                    .map(k -> k.substring(binding.key().length() + 1)).sorted().collect(java.util.stream.Collectors.joining(", ")));
            result.put(slotId(entry.getKey()), Map.copyOf(values));
        }
        snapshots.put(player, Map.copyOf(result));
    }
    public void remove(UUID player) { snapshots.remove(player); }
    public void clear() { snapshots.clear(); }
    public String value(UUID player, String parameter) {
        if (parameter.startsWith("integration_")) return integrations.get(parameter.substring("integration_".length()));
        var snapshot = player == null ? Map.<String, Map<String, String>>of() : snapshots.getOrDefault(player, Map.of());
        if (parameter.equals("equipped_count")) return Integer.toString(snapshot.size());
        int split = parameter.indexOf('_');
        if (split <= 0 || !SLOTS.containsKey(parameter.substring(0, split))) return null;
        String field = parameter.substring(split + 1);
        if (!Set.of("present", "name", "key", "seed", "lineage", "generation", "evolution", "reputation", "abilities", "passives").contains(field)) return null;
        var item = snapshot.get(parameter.substring(0, split));
        if (item != null) return item.get(field);
        return switch (field) { case "present" -> "false"; case "generation", "reputation", "seed" -> "0"; default -> ""; };
    }
}
