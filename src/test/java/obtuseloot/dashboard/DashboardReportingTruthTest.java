package obtuseloot.dashboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DashboardReportingTruthTest {

    @TempDir
    Path temp;

    @Test
    void metricsExposeLabeledDataSources() throws Exception {
        seedAnalytics(temp);
        DashboardService service = new DashboardService(temp);

        DashboardMetrics metrics = service.calculateMetrics();

        assertFalse(metrics.dataSources().isEmpty());
        assertTrue(metrics.dataSources().stream().anyMatch(d -> d.id().equals("ecosystem-balance")
                && d.sourceKind() == DashboardDataSourceDescriptor.DashboardSourceKind.SIMULATION_SNAPSHOT
                && d.authoritative()));
        assertTrue(metrics.dataSources().stream().anyMatch(d -> d.id().equals("ecology-diagnostic")
                && d.authoritative()));
    }

    @Test
    void regenerateDashboardWritesSourceMetadataCompanion() throws Exception {
        seedAnalytics(temp);
        DashboardService service = new DashboardService(temp);

        Path dashboard = service.regenerateDashboard();
        Path metadataPath = dashboard.resolveSibling(dashboard.getFileName() + ".meta.json");

        assertTrue(Files.exists(dashboard));
        assertTrue(Files.exists(metadataPath));
        String meta = Files.readString(metadataPath);
        assertTrue(meta.contains("\"reportType\": \"runtime-dashboard\""));
        assertTrue(meta.contains("\"id\":\"ecosystem-balance\""));
        assertTrue(meta.contains("\"sourceKind\":\"SIMULATION_SNAPSHOT\""));
        assertTrue(meta.contains("\"authoritative\":true"));
    }

    private void seedAnalytics(Path root) throws Exception {
        Files.createDirectories(root.resolve("world-lab"));
        Files.createDirectories(root.resolve("dashboard"));

        Files.writeString(root.resolve("ecosystem-balance-data.json"), """
                {
                  "familyDistribution": {"vanguard": 4, "harbinger": 2},
                  "branchDistribution": {"vanguard.alpha": 3, "harbinger.beta": 2},
                  "triggerDiversity": {"ON_HIT": 5},
                  "mechanicDiversity": {"PULSE": 3}
                }
                """);
        Files.writeString(root.resolve("ecosystem-health-gauge.json"), """
                {
                  "END_artifacts": 5.0,
                  "END_species": 3.0,
                  "END_trend": [4.0, 5.0],
                  "TNT_trend": [0.2, 0.3],
                  "NSER_trend": [0.1, 0.2],
                  "PNNC_current": 2,
                  "PNNC_trend": [1, 2],
                  "interpretation": "healthy",
                  "ecosystem_status": "HEALTHY_ECOSYSTEM"
                }
                """);
        Files.writeString(root.resolve("ecology-diagnostic-state.json"), """
                {
                  "diagnostic_state": "HEALTHY_MULTI_ATTRACTOR",
                  "confidence": 0.91,
                  "warning_flags": ["none"]
                }
                """);
        Files.writeString(root.resolve("world-lab").resolve("world-sim-data.json"), """
                {
                  "ecological_memory": {
                    "active": true,
                    "attractorDuration": 3.2,
                    "memoryPressure": 0.44
                  }
                }
                """);
    }
}
