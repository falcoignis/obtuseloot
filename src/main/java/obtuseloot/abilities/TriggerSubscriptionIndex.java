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
    private final Map<UUID, PlayerArtifactTriggerMap> byPlayer = new ConcurrentHashMap<>();
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
        PlayerArtifactTriggerMap map = PlayerArtifactTriggerMap.fromBindings(bindings, reason, System.nanoTime() - start);
        byPlayer.put(playerId, map);
        rebuildCount.increment();
        rebuildNanos.add(map.rebuildNanos());
        return map;
    }

    public PlayerArtifactTriggerMap get(UUID playerId) {
        return byPlayer.get(playerId);
    }

    public PlayerArtifactTriggerMap getOrRebuild(UUID playerId,
                                                 Artifact artifact,
                                                 ArtifactReputation reputation,
                                                 ItemAbilityManager manager,
                                                 String reason) {
        return byPlayer.computeIfAbsent(playerId, key -> rebuild(key, artifact, reputation, manager, reason));
    }

    public void remove(UUID playerId) {
        byPlayer.remove(playerId);
    }

    public void clear() {
        byPlayer.clear();
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
