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
        Path datasetDir = tempDir.resolve("dataset");
        Path telemetryDir = datasetDir.resolve("telemetry");
        Files.createDirectories(telemetryDir);
        Path rollupDir = telemetryDir;

        new EcosystemHistoryArchive(telemetryDir.resolve("ecosystem-events.log")).append(List.of(
                new EcosystemTelemetryEvent(1_000L, EcosystemTelemetryEventType.ABILITY_EXECUTION, 1L, "lin-a", "SCOUT", Map.of()),
                new EcosystemTelemetryEvent(2_000L, EcosystemTelemetryEventType.ABILITY_EXECUTION, 2L, "lin-b", "RITUAL", Map.of())
        ));

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

        Path harnessDir = datasetDir;
        Files.writeString(harnessDir.resolve("scenario-metadata.properties"), "scenario=chaos-sim\nrun=42\n");

        EcosystemAnalysisJob job = new EcosystemAnalysisJob("job-e2e", datasetDir,
                telemetryDir.resolve("ecosystem-events.log"), rollupDir, harnessDir,
                tempDir.resolve("out"), HistoricalBucketPolicy.rollingSnapshots(3));

        AnalyticsOutputBundle bundle = new EcosystemAnalyticsRunner().run(job);

        assertEquals("job-e2e", bundle.jobId());
        assertTrue(bundle.report().anomalyReport().anomalySeverityScore() > 0.0D);
        assertFalse(bundle.report().anomalyReport().runawayLineages().isEmpty());
        assertEquals(RecommendationDecision.PROPOSED, bundle.recommendationRecord().decision());
        assertTrue(Files.exists(bundle.reportPath()));
        String reportText = Files.readString(bundle.reportPath());
        assertTrue(reportText.contains("branch_survival_half_life="));
        assertTrue(reportText.contains("estimate_status="));
        assertTrue(reportText.contains("cohorts_measured="));
        assertTrue(Files.exists(bundle.recommendationHistoryPath()));
        assertTrue(Files.exists(bundle.jobRecordPath()));
        assertTrue(Files.exists(bundle.runMetadataPath()));
        assertTrue(Files.exists(bundle.outputManifestPath()));
        assertFalse(bundle.report().longTermEvolutionReport().windowDeltas().isEmpty());

        RecommendationHistoryStore store = new RecommendationHistoryStore(bundle.recommendationHistoryPath());
        TuningRecommendationRecord accepted = store.setDecision(bundle.recommendationRecord().recommendationId(), RecommendationDecision.ACCEPTED, "approved in test").orElseThrow();
        Path exported = new TuningProfileExport().exportAccepted(accepted, tempDir.resolve("out/job-e2e-tuning-profile.properties"));
        assertTrue(Files.exists(exported));
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
                Map.of(EcosystemTelemetryEventType.ABILITY_EXECUTION, 10L),
                nicheRollup,
                lineageRollup,
                nichePopulation.values().stream().mapToLong(Long::longValue).sum(),
                0.7D,
                diversity,
                turnover,
                lineageBranches.values().stream().mapToLong(Long::longValue).sum(),
                0L,
                pressure,
                List.of(),
                0L,
                Map.of());

        return new TelemetryRollupSnapshot(TelemetryRollupSnapshot.CURRENT_VERSION, ts, "test", ecosystem);
    }
}
