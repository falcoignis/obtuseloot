package obtuseloot.simulation.worldlab;

import obtuseloot.analytics.ecosystem.AnalyticsInputDataset;
import obtuseloot.analytics.ecosystem.AnalyticsCliMain;
import obtuseloot.analytics.ecosystem.TelemetryDatasetContract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorldSimulationHarnessValidationProfileTest {

    @TempDir
    Path tempDir;

    @Test
    void validationProfileProducesRequiredArtifactsAndRemainsAnalyticsIngestible() throws Exception {
        Path output = tempDir.resolve("world");
        WorldSimulationConfig config = new WorldSimulationConfig(
                42L,
                3,
                2,
                2,
                1,
                0.1D,
                2,
                0.05D,
                0.05D,
                0.7D,
                0.7D,
                output.toString(),
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                FitnessSharingConfig.defaults(),
                SpeciesNicheAnalyticsEngine.BehavioralProjectionConfig.defaults(),
                SpeciesNicheAnalyticsEngine.RoleBasedRepulsionConfig.defaults(),
                SpeciesNicheAnalyticsEngine.MinimumRoleSeparationConfig.defaults(),
                AdaptiveNicheCapacityConfig.defaults(),
                OpportunityWeightedMutationConfig.defaults(),
                true,
                obtuseloot.abilities.ScoringMode.PROJECTION_WITH_CACHE,
                "");

        new WorldSimulationHarness(config).runAndWriteOutputs();

        assertTrue(Files.exists(output.resolve("world-sim-data.json")));
        assertTrue(Files.exists(output.resolve("world-sim-report.md")));
        assertTrue(Files.exists(output.resolve("telemetry").resolve("ecosystem-events.log")));
        assertTrue(Files.exists(output.resolve("telemetry").resolve("rollup-snapshot.properties")));
        assertTrue(Files.exists(output.resolve("rollup_history")));
        assertTrue(Files.exists(output.resolve("scenario-metadata.properties")));

        String worldData = Files.readString(output.resolve("world-sim-data.json"));
        assertTrue(worldData.contains("\"validation_profile\": true"));
        assertFalse(worldData.contains("phase6_experiment_outputs"));

        String metaShift = Files.readString(output.resolve("world-sim-meta-shifts.md"));
        assertTrue(metaShift.contains("Validation profile enabled"));

        TelemetryDatasetContract contract = new TelemetryDatasetContract();
        AnalyticsInputDataset dataset = contract.resolve(output);
        contract.validate(dataset);
        assertEquals(AnalyticsInputDataset.SourceKind.HARNESS, dataset.sourceKind());
        assertEquals(output.resolve("telemetry").resolve("ecosystem-events.log"), dataset.telemetryArchivePath());
        assertEquals(output.resolve("rollup_history"), dataset.rollupSnapshotDirectory());
    }

    @Test
    void analyticsCliIngestsFreshValidationProfileDataset() throws Exception {
        Path output = tempDir.resolve("world-cli");
        Path analyticsOut = tempDir.resolve("analytics-out");

        WorldSimulationConfig config = new WorldSimulationConfig(
                77L,
                3,
                2,
                2,
                1,
                0.1D,
                2,
                0.05D,
                0.05D,
                0.7D,
                0.7D,
                output.toString(),
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                FitnessSharingConfig.defaults(),
                SpeciesNicheAnalyticsEngine.BehavioralProjectionConfig.defaults(),
                SpeciesNicheAnalyticsEngine.RoleBasedRepulsionConfig.defaults(),
                SpeciesNicheAnalyticsEngine.MinimumRoleSeparationConfig.defaults(),
                AdaptiveNicheCapacityConfig.defaults(),
                OpportunityWeightedMutationConfig.defaults(),
                true,
                obtuseloot.abilities.ScoringMode.PROJECTION_WITH_CACHE,
                "");

        new WorldSimulationHarness(config).runAndWriteOutputs();

        AnalyticsCliMain.main(new String[]{
                "analyze",
                "--dataset", output.toString(),
                "--output", analyticsOut.toString(),
                "--job-id", "validation-profile-e2e"
        });

        assertTrue(Files.exists(analyticsOut.resolve("validation-profile-e2e-job-record.properties")));
        assertTrue(Files.exists(analyticsOut.resolve("validation-profile-e2e-output-manifest.properties")));
        assertTrue(Files.exists(analyticsOut.resolve("validation-profile-e2e-run-metadata.properties")));
        assertTrue(Files.exists(analyticsOut.resolve("validation-profile-e2e-analysis-report.txt")));
        assertTrue(Files.exists(analyticsOut.resolve("recommendation-history.log")));

        String runMetadata = Files.readString(analyticsOut.resolve("validation-profile-e2e-run-metadata.properties"));
        assertTrue(runMetadata.contains("status=SUCCESS"));
    }
}
