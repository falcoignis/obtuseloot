package obtuseloot.memory;

import java.util.EnumMap;
import java.util.Map;

public class ArtifactMemory {
    private final EnumMap<ArtifactMemoryEvent, Integer> events = new EnumMap<>(ArtifactMemoryEvent.class);

    public void record(ArtifactMemoryEvent event) { events.merge(event, 1, Integer::sum); }
    public int count(ArtifactMemoryEvent event) { return events.getOrDefault(event, 0); }
    public Map<ArtifactMemoryEvent, Integer> snapshot() { return Map.copyOf(events); }
    public int pressure() { return events.values().stream().mapToInt(Integer::intValue).sum(); }
}
