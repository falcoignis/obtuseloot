package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EcosystemAnalyticsRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void runsEndToEndPipelineWithGovernanceAndExport() throws Exception {
        Path rollupDir = tempDir.resolve("rollups");
        Files.createDirectories(rollupDir);
        new TelemetryRollupSnapshotStore(rollupDir.resolve("001.properties")).write(snapshot(1_000L,
                Map.of("SCOUT", 100L, "RITUAL", 90L), Map.of("SCOUT", 0.8D, "RITUAL", 0.7D),
                Map.of("lin-a", 100L, "lin-b", 80L), Map.of("lin-a", 2L, "lin-b", 2L),
                Map.of("lin-a", 0.03D, "lin-b", 0.02D), 0.72D, 0.12D, Map.of("SCOUT", 10L, "RITUAL", 10L)));
        new TelemetryRollupSnapshotStore(rollupDir.resolve("002.properties")).write(snapshot(2_000L,
                Map.of("SCOUT", 180L, "RITUAL", 50L), Map.of("SCOUT", 1.1D, "RITUAL", 0.4D),
                Map.of("lin-a", 190L, "lin-b", 40L), Map.of("lin-a", 5L, "lin-b", 1L),
                Map.of("lin-a", 0.30D, "lin-b", 0.01D), 0.64D, 0.22D, Map.of("SCOUT", 22L, "RITUAL", 4L)));
        new TelemetryRollupSnapshotStore(rollupDir.resolve("003.properties")).write(snapshot(3_000L,
                Map.of("SCOUT", 280L, "RITUAL", 20L, "FORGE", 15L), Map.of("SCOUT", 1.2D, "RITUAL", 0.1D, "FORGE", 0.5D),
                Map.of("lin-a", 280L, "lin-b", 20L), Map.of("lin-a", 8L, "lin-b", 1L),
                Map.of("lin-a", 0.45D, "lin-b", 0.005D), 0.58D, 0.31D, Map.of("SCOUT", 30L, "RITUAL", 2L, "FORGE", 3L)));

        Path harnessDir = tempDir.resolve("harness");
        Files.createDirectories(harnessDir);
        Files.writeString(harnessDir.resolve("scenario-metadata.properties"), "scenario=chaos-sim\nrun=42\n");

        EcosystemAnalysisJob job = new EcosystemAnalysisJob("job-e2e", null, rollupDir, harnessDir,
                tempDir.resolve("out"), HistoricalBucketPolicy.rollingSnapshots(3));

        AnalyticsOutputBundle bundle = new EcosystemAnalyticsRunner().run(job);

        assertEquals("job-e2e", bundle.jobId());
        assertTrue(bundle.report().anomalyReport().anomalySeverityScore() > 0.0D);
        assertFalse(bundle.report().anomalyReport().runawayLineages().isEmpty());
        assertEquals(RecommendationDecision.PROPOSED, bundle.recommendationRecord().decision());
        assertTrue(Files.exists(bundle.reportPath()));
        assertTrue(Files.exists(bundle.recommendationHistoryPath()));

        RecommendationHistoryStore store = new RecommendationHistoryStore(bundle.recommendationHistoryPath());
        TuningRecommendationRecord accepted = store.setDecision(bundle.recommendationRecord().recommendationId(), RecommendationDecision.ACCEPTED, "approved in test").orElseThrow();
        Path exported = new TuningProfileExport().exportAccepted(accepted, tempDir.resolve("out/job-e2e-tuning-profile.properties"));
        assertTrue(Files.exists(exported));

        String profile = Files.readString(exported);
        assertTrue(profile.contains("ecosystem.parameters.mutationAmplitudeMin"));
        assertTrue(profile.contains("metadata.sourceAnalysisJobId=job-e2e"));
    }

    @Test
    void repeatedRunsProduceComparableBundles() {
        EcosystemAnomalyDetector detector = new EcosystemAnomalyDetector();
        NicheEvolutionReport niche = new NicheEvolutionReport(Map.of("SCOUT", 2.0D), Map.of("SCOUT", 0.4D), Map.of(), List.of("SCOUT"), List.of(), 0.1D, 0.2D);
        LineageSuccessReport lineage = new LineageSuccessReport(Map.of("lin-a", 3.0D), Map.of("lin-a", 4.0D), Map.of("lin-a", 0.01D), List.of(), List.of("lin-a"));

        EcosystemAnomalyReport a1 = detector.detect(niche, lineage, List.of(snapshot(1_000L, Map.of("SCOUT", 100L), Map.of("SCOUT", 1.0D), Map.of("lin-a", 100L), Map.of("lin-a", 2L), Map.of("lin-a", 0.03D), 0.7D, 0.2D, Map.of()), snapshot(2_000L, Map.of("SCOUT", 120L), Map.of("SCOUT", 1.1D), Map.of("lin-a", 120L), Map.of("lin-a", 3L), Map.of("lin-a", 0.04D), 0.68D, 0.22D, Map.of()), snapshot(3_000L, Map.of("SCOUT", 140L), Map.of("SCOUT", 1.2D), Map.of("lin-a", 180L), Map.of("lin-a", 5L), Map.of("lin-a", 0.05D), 0.60D, 0.30D, Map.of())));
        EcosystemAnomalyReport a2 = detector.detect(niche, lineage, List.of(snapshot(1_500L, Map.of("SCOUT", 110L), Map.of("SCOUT", 1.0D), Map.of("lin-a", 110L), Map.of("lin-a", 2L), Map.of("lin-a", 0.03D), 0.7D, 0.2D, Map.of()), snapshot(2_500L, Map.of("SCOUT", 130L), Map.of("SCOUT", 1.1D), Map.of("lin-a", 130L), Map.of("lin-a", 3L), Map.of("lin-a", 0.04D), 0.68D, 0.22D, Map.of()), snapshot(3_500L, Map.of("SCOUT", 160L), Map.of("SCOUT", 1.2D), Map.of("lin-a", 190L), Map.of("lin-a", 6L), Map.of("lin-a", 0.05D), 0.60D, 0.30D, Map.of())));

        assertEquals(a1.baselineMetrics().get("runaway_success_rate_threshold"),
                a2.baselineMetrics().get("runaway_success_rate_threshold"), 0.25D);
    }

    private TelemetryRollupSnapshot snapshot(long ts,
                                             Map<String, Long> nichePopulation,
                                             Map<String, Double> nicheUtility,
                                             Map<String, Long> lineagePopulation,
                                             Map<String, Long> lineageBranches,
                                             Map<String, Double> specializationTrajectory,
                                             double diversity,
                                             double turnover,
                                             Map<String, Long> pressure) {
        NichePopulationRollup nicheRollup = new NichePopulationRollup(ts, nichePopulation, nichePopulation, nicheUtility,
                Map.of(), Map.of(), Map.of(), Map.of());
        LineagePopulationRollup lineageRollup = new LineagePopulationRollup(ts, lineagePopulation, lineageBranches,
                Map.of(), Map.of(), specializationTrajectory, Map.of(), Map.of(), Map.of());
        EcosystemSnapshot ecosystem = new EcosystemSnapshot(ts,
                Map.of(EcosystemTelemetryEventType.ABILITY_EXECUTION, 10L), nicheRollup, lineageRollup,
                nichePopulation.values().stream().mapToLong(Long::longValue).sum(), 0.7D, diversity, turnover,
                lineageBranches.values().stream().mapToLong(Long::longValue).sum(), 0L, pressure);
        return new TelemetryRollupSnapshot(TelemetryRollupSnapshot.CURRENT_VERSION, ts, "test", ecosystem);
    }
}
