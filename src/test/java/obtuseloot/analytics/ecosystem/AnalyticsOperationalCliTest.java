package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsOperationalCliTest {

    @TempDir
    Path tempDir;

    @Test
    void cliAnalyzeAndGovernanceWorkflowEndToEnd() throws Exception {
        Path dataset = tempDir.resolve("harness-out");
        Path telemetry = dataset.resolve("telemetry");
        Files.createDirectories(telemetry);

        new EcosystemHistoryArchive(telemetry.resolve("ecosystem-events.log")).append(List.of(
                new EcosystemTelemetryEvent(1_000L, EcosystemTelemetryEventType.ABILITY_EXECUTION, 11L, "lin-a", "SCOUT", Map.of("generation", "1"))
        ));
        new TelemetryRollupSnapshotStore(telemetry.resolve("rollup-snapshot.properties")).write(snapshot(1_000L, 0.72D, 0.12D));
        new TelemetryRollupSnapshotStore(telemetry.resolve("rollup-002.properties")).write(snapshot(2_000L, 0.63D, 0.18D));
        new TelemetryRollupSnapshotStore(telemetry.resolve("rollup-003.properties")).write(snapshot(3_000L, 0.58D, 0.22D));
        Files.writeString(dataset.resolve("scenario-metadata.properties"), "scenario=ops-e2e\n");

        Path out = tempDir.resolve("analysis-out");
        AnalyticsCliMain.main(new String[]{"analyze", "--dataset", dataset.toString(), "--output", out.toString(), "--job-id", "ops-e2e"});

        Path historyPath = out.resolve("recommendation-history.log");
        assertTrue(Files.exists(historyPath));

        RecommendationHistoryStore store = new RecommendationHistoryStore(historyPath);
        TuningRecommendationRecord latest = store.latest().orElseThrow();

        Path exportDir = out.resolve("accepted");
        AnalyticsCliMain.main(new String[]{"decide", "--history", historyPath.toString(), "--recommendation-id", latest.recommendationId(), "--decision", "ACCEPTED", "--export-dir", exportDir.toString()});

        assertTrue(Files.exists(out.resolve("ops-e2e-output-manifest.properties")));
        assertTrue(Files.exists(exportDir.resolve(latest.recommendationId() + "-tuning-profile.properties")));
    }

    @Test
    void supportsRunSpecForCronFriendlyAutomation() throws Exception {
        Path dataset = tempDir.resolve("runtime-output");
        Path telemetry = dataset.resolve("telemetry");
        Files.createDirectories(telemetry);
        new EcosystemHistoryArchive(telemetry.resolve("ecosystem-events.log")).append(List.of(
                new EcosystemTelemetryEvent(1_000L, EcosystemTelemetryEventType.ABILITY_EXECUTION, 11L, "lin-a", "SCOUT", Map.of())
        ));
        new TelemetryRollupSnapshotStore(telemetry.resolve("rollup-snapshot.properties")).write(snapshot(1_000L, 0.70D, 0.15D));
        new TelemetryRollupSnapshotStore(telemetry.resolve("rollup-002.properties")).write(snapshot(2_000L, 0.69D, 0.16D));
        new TelemetryRollupSnapshotStore(telemetry.resolve("rollup-003.properties")).write(snapshot(3_000L, 0.67D, 0.17D));

        Path out = tempDir.resolve("cron-out");
        Path spec = tempDir.resolve("daily.properties");
        Files.writeString(spec, String.join("\n",
                "job.id=daily-analytics",
                "dataset.path=" + dataset,
                "output.path=" + out,
                "bucket.type=ROLLING_WINDOW",
                "bucket.retention=3",
                "bucket.span=1",
                "bucket.rollingWindowSnapshots=3"
        ));

        AnalyticsCliMain.main(new String[]{"run-spec", "--spec", spec.toString()});

        assertTrue(Files.exists(out.resolve("daily-analytics-analysis-report.txt")));
        assertTrue(Files.exists(out.resolve("daily-analytics-run-metadata.properties")));
    }

    @Test
    void datasetContractHandlesRuntimeAndHarnessLayouts() {
        TelemetryDatasetContract contract = new TelemetryDatasetContract();

        Path runtime = tempDir.resolve("runtime");
        assertThrows(IllegalArgumentException.class, () -> contract.resolve(runtime));

        Path runtimeTelemetry = runtime.resolve("telemetry");
        assertDoesNotThrow(() -> Files.createDirectories(runtimeTelemetry));
        new EcosystemHistoryArchive(runtimeTelemetry.resolve("ecosystem-events.log")).append(List.of(
                new EcosystemTelemetryEvent(1_000L, EcosystemTelemetryEventType.ABILITY_EXECUTION, 1L, "lin", "SCOUT", Map.of())
        ));
        new TelemetryRollupSnapshotStore(runtimeTelemetry.resolve("rollup-snapshot.properties")).write(snapshot(1_000L, 0.5D, 0.1D));
        AnalyticsInputDataset runtimeDataset = contract.resolve(runtime);
        assertEquals(AnalyticsInputDataset.SourceKind.RUNTIME, runtimeDataset.sourceKind());

        Path harness = tempDir.resolve("harness");
        Path harnessTelemetry = harness.resolve("telemetry");
        assertDoesNotThrow(() -> Files.createDirectories(harnessTelemetry));
        new EcosystemHistoryArchive(harnessTelemetry.resolve("ecosystem-events.log")).append(List.of(
                new EcosystemTelemetryEvent(1_100L, EcosystemTelemetryEventType.ABILITY_EXECUTION, 2L, "lin", "SCOUT", Map.of())
        ));
        new TelemetryRollupSnapshotStore(harnessTelemetry.resolve("rollup-snapshot.properties")).write(snapshot(1_100L, 0.5D, 0.1D));
        assertDoesNotThrow(() -> Files.writeString(harness.resolve("scenario-metadata.properties"), "scenario=test\n"));
        assertEquals(AnalyticsInputDataset.SourceKind.HARNESS, contract.resolve(harness).sourceKind());

        Path harnessWithHistory = tempDir.resolve("harness-history");
        Path harnessHistoryTelemetry = harnessWithHistory.resolve("telemetry");
        Path rollupHistory = harnessWithHistory.resolve("rollup_history");
        assertDoesNotThrow(() -> Files.createDirectories(harnessHistoryTelemetry));
        assertDoesNotThrow(() -> Files.createDirectories(rollupHistory));
        new EcosystemHistoryArchive(harnessHistoryTelemetry.resolve("ecosystem-events.log")).append(List.of(
                new EcosystemTelemetryEvent(1_200L, EcosystemTelemetryEventType.ABILITY_EXECUTION, 3L, "lin", "SCOUT", Map.of())
        ));
        new TelemetryRollupSnapshotStore(rollupHistory.resolve("rollup-001.properties")).write(snapshot(1_200L, 0.55D, 0.11D));
        new TelemetryRollupSnapshotStore(rollupHistory.resolve("rollup-002.properties")).write(snapshot(1_300L, 0.57D, 0.13D));
        assertDoesNotThrow(() -> Files.writeString(harnessWithHistory.resolve("scenario-metadata.properties"), "scenario=test-history\n"));
        AnalyticsInputDataset historyDataset = contract.resolve(harnessWithHistory);
        assertEquals(AnalyticsInputDataset.SourceKind.HARNESS, historyDataset.sourceKind());
        assertEquals(rollupHistory.toAbsolutePath().normalize(), historyDataset.rollupSnapshotDirectory().toAbsolutePath().normalize());
    }

    private TelemetryRollupSnapshot snapshot(long ts, double diversity, double turnover) {
        NichePopulationRollup niche = new NichePopulationRollup(ts, Map.of("SCOUT", 10L), Map.of("SCOUT", 10L), Map.of("SCOUT", 1.0D), Map.of(), Map.of(), Map.of(), Map.of());
        LineagePopulationRollup lineage = new LineagePopulationRollup(ts, Map.of("lin-a", 10L), Map.of("lin-a", 2L), Map.of(), Map.of(), Map.of("lin-a", 0.02D), Map.of(), Map.of(), Map.of());
        EcosystemSnapshot ecosystem = new EcosystemSnapshot(ts, Map.of(EcosystemTelemetryEventType.ABILITY_EXECUTION, 10L), niche, lineage, 10L, 0.5D, diversity, turnover, 2L, 0L, Map.of("SCOUT", 1L));
        return new TelemetryRollupSnapshot(TelemetryRollupSnapshot.CURRENT_VERSION, ts, "test", ecosystem);
    }
}
