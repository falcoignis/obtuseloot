package obtuseloot.telemetry;

import java.util.List;

public class RollupStateHydrator {
    private final TelemetryRollupSnapshotStore snapshotStore;
    private final EcosystemHistoryArchive archive;
    private final int replayWindowEvents;

    public RollupStateHydrator(TelemetryRollupSnapshotStore snapshotStore,
                               EcosystemHistoryArchive archive,
                               int replayWindowEvents) {
        this.snapshotStore = snapshotStore;
        this.archive = archive;
        this.replayWindowEvents = Math.max(1, replayWindowEvents);
    }

    public RehydrationResult rehydrate(TelemetryAggregationBuffer buffer, ScheduledEcosystemRollups rollups) {
        long started = System.currentTimeMillis();
        var snapshotOpt = snapshotStore.readLatest();
        if (snapshotOpt.isPresent()) {
            TelemetryRollupSnapshot snapshot = snapshotOpt.get();
            buffer.rehydrateFrom(snapshot.ecosystemSnapshot());
            rollups.restore(snapshot.ecosystemSnapshot(), snapshot.ecosystemSnapshot().generatedAtMs());
            return new RehydrationResult("rehydrated_snapshot", 0, started, System.currentTimeMillis(), snapshot.ecosystemSnapshot().generatedAtMs());
        }

        List<EcosystemTelemetryEvent> replay;
        try {
            replay = archive.readRecent(replayWindowEvents);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Bad persisted telemetry event encountered during replay rehydration", ex);
        }
        for (EcosystemTelemetryEvent event : replay) {
            buffer.enqueue(event);
            buffer.drain(1);
        }
        if (!replay.isEmpty()) {
            long at = replay.get(replay.size() - 1).timestampMs();
            rollups.run(at);
            return new RehydrationResult("rehydrated_replay", replay.size(), started, System.currentTimeMillis(), at);
        }
        return new RehydrationResult("cold_start", 0, started, System.currentTimeMillis(), 0L);
    }

    public record RehydrationResult(String mode,
                                    int replayedEvents,
                                    long startedAtMs,
                                    long completedAtMs,
                                    long restoredSnapshotAtMs) {
        public long durationMs() {
            return Math.max(0L, completedAtMs - startedAtMs);
        }
    }
}
