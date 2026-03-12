package obtuseloot.telemetry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TelemetryAggregationService {
    private final TelemetryAggregationBuffer buffer;
    private final EcosystemHistoryArchive archive;
    private final ScheduledEcosystemRollups rollups;
    private final int archiveBatchSize;
    private final TelemetryRollupSnapshotStore snapshotStore;
    private final RollupStateHydrator hydrator;
    private volatile RollupStateHydrator.RehydrationResult initialization =
            new RollupStateHydrator.RehydrationResult("cold_start", 0, 0L, 0L, 0L);

    public TelemetryAggregationService(TelemetryAggregationBuffer buffer,
                                       EcosystemHistoryArchive archive,
                                       ScheduledEcosystemRollups rollups,
                                       int archiveBatchSize) {
        this(buffer, archive, rollups, archiveBatchSize,
                new TelemetryRollupSnapshotStore(java.nio.file.Path.of("analytics/telemetry/rollup-snapshot.properties")),
                new RollupStateHydrator(new TelemetryRollupSnapshotStore(java.nio.file.Path.of("analytics/telemetry/rollup-snapshot.properties")), archive, 512));
    }

    public TelemetryAggregationService(TelemetryAggregationBuffer buffer,
                                       EcosystemHistoryArchive archive,
                                       ScheduledEcosystemRollups rollups,
                                       int archiveBatchSize,
                                       TelemetryRollupSnapshotStore snapshotStore,
                                       RollupStateHydrator hydrator) {
        this.buffer = buffer;
        this.archive = archive;
        this.rollups = rollups;
        this.archiveBatchSize = archiveBatchSize;
        this.snapshotStore = snapshotStore;
        this.hydrator = hydrator;
    }

    public void initializeFromHistory() {
        initialization = hydrator.rehydrate(buffer, rollups);
    }

    public void record(EcosystemTelemetryEvent event) {
        buffer.enqueue(event);
        if (buffer.pendingCount() >= archiveBatchSize) {
            flush();
        }
    }

    public void flush() {
        List<EcosystemTelemetryEvent> batch = buffer.drain(archiveBatchSize);
        if (!batch.isEmpty()) {
            archive.append(batch);
        }
    }

    public void flushAll() {
        while (buffer.pendingCount() > 0) {
            flush();
        }
    }

    public void scheduledRollupTick(long nowMs) {
        flush();
        ScheduledEcosystemRollups.RollupGeneration generation = rollups.maybeRun(nowMs);
        if (generation.generated()) {
            recordRollupGenerated(generation);
            snapshotStore.write(new TelemetryRollupSnapshot(
                    TelemetryRollupSnapshot.CURRENT_VERSION,
                    nowMs,
                    initialization.mode(),
                    rollups.ecosystemSnapshot()));
        }
    }

    private void recordRollupGenerated(ScheduledEcosystemRollups.RollupGeneration generation) {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("trigger", "scheduled-rollup");
        attrs.put("context_tags", "rollup-generated");
        attrs.put("rollup_type", "ecosystem_snapshot");
        attrs.put("rollup_record_count", String.valueOf(generation.recordCount()));
        attrs.put("rollup_window_ms", String.valueOf(generation.durationMs()));
        attrs.put("snapshot_version", String.valueOf(TelemetryRollupSnapshot.CURRENT_VERSION));
        EcosystemTelemetryEvent event = new TelemetryEventFactory().create(
                EcosystemTelemetryEventType.ROLLUP_GENERATED,
                0L,
                "",
                "",
                attrs);
        archive.append(List.of(event));
    }

    public ScheduledEcosystemRollups rollups() {
        return rollups;
    }

    public EcosystemHistoryArchive archive() {
        return archive;
    }

    public RollupStateHydrator.RehydrationResult initialization() {
        return initialization;
    }
}
