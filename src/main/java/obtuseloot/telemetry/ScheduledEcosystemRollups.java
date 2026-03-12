package obtuseloot.telemetry;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ScheduledEcosystemRollups {
    private final TelemetryAggregationBuffer buffer;
    private final long minIntervalMs;
    private volatile long lastGeneratedAtMs;
    private final AtomicReference<NichePopulationRollup> nicheRollup = new AtomicReference<>(emptyNiche(0L));
    private final AtomicReference<LineagePopulationRollup> lineageRollup = new AtomicReference<>(emptyLineage(0L));
    private final AtomicReference<EcosystemSnapshot> snapshot = new AtomicReference<>(
            new EcosystemSnapshot(0L, Map.of(), nicheRollup.get(), lineageRollup.get(), 0L, 0.0D, 0.0D, 0.0D, 0L, 0L, Map.of()));

    public ScheduledEcosystemRollups(TelemetryAggregationBuffer buffer, long minIntervalMs) {
        this.buffer = buffer;
        this.minIntervalMs = minIntervalMs;
    }

    public RollupGeneration maybeRun(long nowMs) {
        if (nowMs - lastGeneratedAtMs < minIntervalMs) {
            return RollupGeneration.skipped(nowMs);
        }
        return run(nowMs);
    }

    public RollupGeneration run(long nowMs) {
        long before = System.nanoTime();
        NichePopulationRollup niche = new NichePopulationRollup(
                nowMs,
                buffer.nichePopulationSnapshot(),
                buffer.meaningfulByNicheSnapshot(),
                buffer.utilityDensityByNicheSnapshot(),
                buffer.saturationPressureByNicheSnapshot(),
                buffer.opportunityShareByNicheSnapshot(),
                buffer.specializationPressureByNicheSnapshot(),
                buffer.branchContributionByNicheSnapshot());

        LineagePopulationRollup lineage = new LineagePopulationRollup(
                nowMs,
                buffer.lineagePopulationSnapshot(),
                buffer.branchCountByLineageSnapshot(),
                buffer.utilityDensityByLineageSnapshot(),
                buffer.momentumByLineageSnapshot(),
                buffer.specializationTrajectoryByLineageSnapshot(),
                buffer.nicheDistributionByLineageSnapshot(),
                buffer.driftWindowByLineageSnapshot(),
                buffer.branchDivergenceByLineageSnapshot());

        long activeArtifactCount = buffer.activeArtifactCountSnapshot();
        long branchBirthCount = buffer.branchBirthCountSnapshot();
        long branchCollapseCount = buffer.branchCollapseCountSnapshot();
        double capacityUtilization = activeArtifactCount == 0 ? 0.0D
                : Math.min(1.0D, activeArtifactCount / Math.max(1.0D, activeArtifactCount + niche.populationByNiche().size()));
        double diversity = niche.populationByNiche().isEmpty() ? 0.0D
                : ((double) niche.populationByNiche().size() / Math.max(1.0D, activeArtifactCount));
        double turnover = activeArtifactCount == 0 ? 0.0D
                : (branchBirthCount + branchCollapseCount) / (double) activeArtifactCount;

        EcosystemSnapshot next = new EcosystemSnapshot(
                nowMs,
                buffer.typeCountsSnapshot(),
                niche,
                lineage,
                activeArtifactCount,
                capacityUtilization,
                diversity,
                turnover,
                branchBirthCount,
                branchCollapseCount,
                buffer.competitionPressureDistributionSnapshot());
        nicheRollup.set(niche);
        lineageRollup.set(lineage);
        snapshot.set(next);
        lastGeneratedAtMs = nowMs;
        long durationMs = Math.max(0L, (System.nanoTime() - before) / 1_000_000L);
        long recordCount = next.eventCounts().values().stream().mapToLong(Long::longValue).sum();
        return RollupGeneration.generated(nowMs, durationMs, recordCount);
    }

    public synchronized void restore(EcosystemSnapshot restored, long restoredAtMs) {
        if (restored == null) {
            return;
        }
        nicheRollup.set(restored.nichePopulationRollup());
        lineageRollup.set(restored.lineagePopulationRollup());
        snapshot.set(restored);
        lastGeneratedAtMs = Math.max(lastGeneratedAtMs, restoredAtMs);
    }

    public NichePopulationRollup nichePopulationRollup() { return nicheRollup.get(); }
    public LineagePopulationRollup lineagePopulationRollup() { return lineageRollup.get(); }
    public EcosystemSnapshot ecosystemSnapshot() { return snapshot.get(); }

    private static NichePopulationRollup emptyNiche(long ts) {
        return new NichePopulationRollup(ts, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    private static LineagePopulationRollup emptyLineage(long ts) {
        return new LineagePopulationRollup(ts, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    public record RollupGeneration(boolean generated,
                                   long atMs,
                                   long durationMs,
                                   long recordCount) {
        static RollupGeneration skipped(long atMs) {
            return new RollupGeneration(false, atMs, 0L, 0L);
        }

        static RollupGeneration generated(long atMs, long durationMs, long recordCount) {
            return new RollupGeneration(true, atMs, durationMs, recordCount);
        }
    }
}
