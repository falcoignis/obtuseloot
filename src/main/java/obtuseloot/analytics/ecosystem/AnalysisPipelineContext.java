package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.EcosystemTelemetryEvent;
import obtuseloot.telemetry.TelemetryRollupSnapshot;

import java.util.List;
import java.util.Map;

public record AnalysisPipelineContext(
        EcosystemAnalysisJob job,
        List<EcosystemTelemetryEvent> telemetryEvents,
        List<TelemetryRollupSnapshot> rollupHistory,
        Map<String, String> scenarioMetadata,
        List<TelemetryRollupSnapshot> selectedWindow
) {
}
