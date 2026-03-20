package obtuseloot.memory;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactArchetypeValidator;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ArtifactMemoryEngine {
    private static final long MEMORY_TRIGGER_MIN_INTERVAL_MS = 1750L;

    private final MemoryInfluenceResolver influenceResolver = new MemoryInfluenceResolver();
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();
    private final Map<String, MemoryTriggerSnapshot> memoryTriggerSnapshots = new ConcurrentHashMap<>();

    public ArtifactMemoryProfile recordAndProfile(Artifact artifact, ArtifactMemoryEvent event) {
        ArtifactArchetypeValidator.requireValid(artifact, "memory recording");
        artifact.getMemory().record(event);
        artifact.addNotableEvent("memory." + event.name().toLowerCase());
        artifact.addLoreHistory(textResolver.compose(artifact, ArtifactTextChannel.MEMORY, event.name().toLowerCase()));
        return influenceResolver.profileFor(artifact.getMemory());
    }

    public boolean shouldEmitMemoryTrigger(Artifact artifact, ArtifactMemoryEvent event, long now) {
        String key = artifact.getArtifactStorageKey();
        int pressure = artifact.getMemory().pressure();
        MemoryTriggerSnapshot previous = memoryTriggerSnapshots.get(key);
        if (previous != null) {
            if (now - previous.lastEmissionAt() < MEMORY_TRIGGER_MIN_INTERVAL_MS) {
                return false;
            }
            if (previous.lastEvent() == event && pressure == previous.lastPressure()) {
                return false;
            }
        }
        memoryTriggerSnapshots.put(key, new MemoryTriggerSnapshot(now, event, pressure));
        return true;
    }

    public ArtifactMemoryProfile profile(Artifact artifact) {
        ArtifactArchetypeValidator.requireValid(artifact, "memory recording");
        return influenceResolver.profileFor(artifact.getMemory());
    }

    private record MemoryTriggerSnapshot(long lastEmissionAt, ArtifactMemoryEvent lastEvent, int lastPressure) {
    }
}
