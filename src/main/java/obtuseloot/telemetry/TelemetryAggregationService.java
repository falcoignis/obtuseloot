package obtuseloot.telemetry;

import java.util.List;

public class TelemetryAggregationService {
    private final TelemetryAggregationBuffer buffer;
    private final EcosystemHistoryArchive archive;
    private final ScheduledEcosystemRollups rollups;
    private final int archiveBatchSize;

    public TelemetryAggregationService(TelemetryAggregationBuffer buffer,
                                       EcosystemHistoryArchive archive,
                                       ScheduledEcosystemRollups rollups,
                                       int archiveBatchSize) {
        this.buffer = buffer;
        this.archive = archive;
        this.rollups = rollups;
        this.archiveBatchSize = archiveBatchSize;
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

    public void scheduledRollupTick(long nowMs) {
        flush();
        rollups.maybeRun(nowMs);
    }

    public ScheduledEcosystemRollups rollups() {
        return rollups;
    }

    public EcosystemHistoryArchive archive() {
        return archive;
    }
}
