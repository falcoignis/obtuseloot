package obtuseloot.abilities;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PlayerArtifactTriggerMap {
    private static final List<ArtifactTriggerBinding> EMPTY_BINDINGS = List.of();

    private final EnumMap<AbilityTrigger, List<ArtifactTriggerBinding>> byTrigger;
    private final long artifactSeed;
    private final String artifactStorageKey;
    private final int totalBindings;
    private final long rebuildNanos;
    private final String lastRebuildReason;

    private PlayerArtifactTriggerMap(EnumMap<AbilityTrigger, List<ArtifactTriggerBinding>> byTrigger,
                                     long artifactSeed,
                                     String artifactStorageKey,
                                     int totalBindings,
                                     long rebuildNanos,
                                     String lastRebuildReason) {
        this.byTrigger = byTrigger;
        this.artifactSeed = artifactSeed;
        this.artifactStorageKey = artifactStorageKey;
        this.totalBindings = totalBindings;
        this.rebuildNanos = rebuildNanos;
        this.lastRebuildReason = lastRebuildReason;
    }

    public static PlayerArtifactTriggerMap fromBindings(List<ArtifactTriggerBinding> bindings,
                                                        long artifactSeed,
                                                        String artifactStorageKey,
                                                        String reason,
                                                        long rebuildNanos) {
        EnumMap<AbilityTrigger, List<ArtifactTriggerBinding>> grouped = new EnumMap<>(AbilityTrigger.class);
        for (AbilityTrigger trigger : AbilityTrigger.values()) {
            grouped.put(trigger, EMPTY_BINDINGS);
        }
        for (ArtifactTriggerBinding binding : bindings) {
            List<ArtifactTriggerBinding> existing = grouped.get(binding.trigger());
            if (existing == EMPTY_BINDINGS) {
                existing = new ArrayList<>();
                grouped.put(binding.trigger(), existing);
            }
            existing.add(binding);
        }
        grouped.replaceAll((k, v) -> v == EMPTY_BINDINGS ? v : List.copyOf(v));
        return new PlayerArtifactTriggerMap(grouped, artifactSeed, artifactStorageKey, bindings.size(), rebuildNanos, reason);
    }

    public List<ArtifactTriggerBinding> bindingsFor(AbilityTrigger trigger) {
        return byTrigger.getOrDefault(trigger, EMPTY_BINDINGS);
    }

    public Map<AbilityTrigger, List<ArtifactTriggerBinding>> asMap() {
        return Map.copyOf(byTrigger);
    }

    public int totalBindings() {
        return totalBindings;
    }

    public long artifactSeed() {
        return artifactSeed;
    }

    public String artifactStorageKey() {
        return artifactStorageKey;
    }

    public boolean matchesArtifactIdentity(long currentArtifactSeed, String currentArtifactStorageKey) {
        return artifactSeed == currentArtifactSeed
                && Objects.equals(artifactStorageKey, currentArtifactStorageKey);
    }

    public long rebuildNanos() {
        return rebuildNanos;
    }

    public String lastRebuildReason() {
        return lastRebuildReason;
    }
}
