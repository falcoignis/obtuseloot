package obtuseloot.telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

public class TelemetryAggregationBuffer {
    private final ConcurrentLinkedQueue<EcosystemTelemetryEvent> pending = new ConcurrentLinkedQueue<>();
    private final Map<String, LongAdder> nichePopulation = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> lineagePopulation = new ConcurrentHashMap<>();
    private final Map<EcosystemTelemetryEventType, LongAdder> typeCounts = new ConcurrentHashMap<>();

    public void enqueue(EcosystemTelemetryEvent event) {
        pending.add(event);
        typeCounts.computeIfAbsent(event.type(), ignored -> new LongAdder()).increment();
        if (event.niche() != null && !event.niche().isBlank()) {
            nichePopulation.computeIfAbsent(event.niche(), ignored -> new LongAdder()).increment();
        }
        if (event.lineageId() != null && !event.lineageId().isBlank()) {
            lineagePopulation.computeIfAbsent(event.lineageId(), ignored -> new LongAdder()).increment();
        }
    }

    public List<EcosystemTelemetryEvent> drain(int maxEvents) {
        List<EcosystemTelemetryEvent> out = new ArrayList<>(Math.max(1, maxEvents));
        for (int i = 0; i < maxEvents; i++) {
            EcosystemTelemetryEvent event = pending.poll();
            if (event == null) {
                break;
            }
            out.add(event);
        }
        return out;
    }

    public int pendingCount() {
        return pending.size();
    }

    public Map<String, Long> nichePopulationSnapshot() {
        return snapshot(nichePopulation);
    }

    public Map<String, Long> lineagePopulationSnapshot() {
        return snapshot(lineagePopulation);
    }

    public Map<EcosystemTelemetryEventType, Long> typeCountsSnapshot() {
        Map<EcosystemTelemetryEventType, Long> out = new ConcurrentHashMap<>();
        typeCounts.forEach((k, v) -> out.put(k, v.sum()));
        return Map.copyOf(out);
    }

    private Map<String, Long> snapshot(Map<String, LongAdder> source) {
        Map<String, Long> out = new ConcurrentHashMap<>();
        source.forEach((k, v) -> out.put(k, v.sum()));
        return Map.copyOf(out);
    }
}
