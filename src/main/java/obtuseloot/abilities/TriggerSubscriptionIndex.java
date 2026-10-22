package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class TriggerSubscriptionIndex {
    private final Map<UUID, Map<String, PlayerArtifactTriggerMap>> byPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerArtifactTriggerMap> latest = new ConcurrentHashMap<>();
    private final LongAdder rebuildCount = new LongAdder();
    private final LongAdder rebuildNanos = new LongAdder();

    public PlayerArtifactTriggerMap rebuild(UUID playerId,
                                            Artifact artifact,
                                            ArtifactReputation reputation,
                                            ItemAbilityManager manager,
                                            String reason) {
        long start = System.nanoTime();
        AbilityProfile profile = manager.profileFor(artifact, reputation);
        List<ArtifactTriggerBinding> bindings = new ArrayList<>(profile.abilities().size());
        for (AbilityDefinition definition : profile.abilities()) {
            bindings.add(new ArtifactTriggerBinding(
                    artifact.getArtifactSeed(),
                    artifact.getGeneratedName(),
                    definition.id(),
                    definition.name(),
                    definition.trigger(),
                    definition
            ));
        }
        PlayerArtifactTriggerMap map = PlayerArtifactTriggerMap.fromBindings(
                bindings,
                artifact.getArtifactSeed(),
                artifact.getArtifactStorageKey(),
                reason,
                System.nanoTime() - start,
                artifact.getGenome(),
                artifact.getGenomeRevision()
        );
        byPlayer.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>()).put(artifact.getArtifactStorageKey(), map);
        latest.put(playerId, map);
        rebuildCount.increment();
        rebuildNanos.add(map.rebuildNanos());
        return map;
    }

    public PlayerArtifactTriggerMap get(UUID playerId, String key) {
        return byPlayer.getOrDefault(playerId, Map.of()).get(key);
    }
    public PlayerArtifactTriggerMap get(UUID playerId) {
        return latest.get(playerId);
    }

    public PlayerArtifactTriggerMap getOrRebuild(UUID playerId,
                                                 Artifact artifact,
                                                 ArtifactReputation reputation,
                                                 ItemAbilityManager manager,
                                                 String reason) {
        PlayerArtifactTriggerMap existing = byPlayer.getOrDefault(playerId, Map.of()).get(artifact.getArtifactStorageKey());
        if (existing != null && existing.matchesArtifactIdentity(artifact.getArtifactSeed(), artifact.getArtifactStorageKey()) && existing.matchesGenome(artifact.getGenome(), artifact.getGenomeRevision())) {
            return existing;
        }
        return rebuild(playerId, artifact, reputation, manager, reason);
    }

    public void remove(UUID playerId) {
        byPlayer.remove(playerId);
        latest.remove(playerId);
    }

    public void remove(UUID playerId, String itemKey) {
        var maps = byPlayer.get(playerId);
        if (maps == null) return;
        var removed = maps.remove(itemKey);
        if (removed != null) latest.remove(playerId, removed);
        if (maps.isEmpty()) byPlayer.remove(playerId, maps);
    }

    public void clear() {
        byPlayer.clear();
        latest.clear();
    }

    public int playerCount() {
        return byPlayer.size();
    }

    public long rebuildCount() {
        return rebuildCount.sum();
    }

    public double averageRebuildMicros() {
        long count = rebuildCount.sum();
        if (count == 0) {
            return 0.0D;
        }
        return (rebuildNanos.sum() / 1_000.0D) / count;
    }
}
