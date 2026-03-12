package obtuseloot.telemetry;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ScheduledEcosystemRollups {
    private final TelemetryAggregationBuffer buffer;
    private final long minIntervalMs;
    private volatile long lastGeneratedAtMs;
    private final AtomicReference<NichePopulationRollup> nicheRollup = new AtomicReference<>(new NichePopulationRollup(0L, Map.of()));
    private final AtomicReference<LineagePopulationRollup> lineageRollup = new AtomicReference<>(new LineagePopulationRollup(0L, Map.of()));
    private final AtomicReference<EcosystemSnapshot> snapshot = new AtomicReference<>(new EcosystemSnapshot(0L, Map.of(), nicheRollup.get(), lineageRollup.get()));

    public ScheduledEcosystemRollups(TelemetryAggregationBuffer buffer, long minIntervalMs) {
        this.buffer = buffer;
        this.minIntervalMs = minIntervalMs;
    }

    public void maybeRun(long nowMs) {
        if (nowMs - lastGeneratedAtMs < minIntervalMs) {
            return;
        }
        run(nowMs);
    }

    public void run(long nowMs) {
        NichePopulationRollup niche = new NichePopulationRollup(nowMs, buffer.nichePopulationSnapshot());
        LineagePopulationRollup lineage = new LineagePopulationRollup(nowMs, buffer.lineagePopulationSnapshot());
        EcosystemSnapshot next = new EcosystemSnapshot(nowMs, buffer.typeCountsSnapshot(), niche, lineage);
        nicheRollup.set(niche);
        lineageRollup.set(lineage);
        snapshot.set(next);
        lastGeneratedAtMs = nowMs;
    }

    public NichePopulationRollup nichePopulationRollup() {
        return nicheRollup.get();
    }

    public LineagePopulationRollup lineagePopulationRollup() {
        return lineageRollup.get();
    }

    public EcosystemSnapshot ecosystemSnapshot() {
        return snapshot.get();
    }
}
