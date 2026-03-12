package obtuseloot.telemetry;

import java.util.Map;

public class EcosystemTelemetryEmitter {
    private final TelemetryAggregationService aggregationService;

    public EcosystemTelemetryEmitter(TelemetryAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    public void emit(EcosystemTelemetryEventType type,
                     long artifactSeed,
                     String lineageId,
                     String niche,
                     Map<String, String> attributes) {
        aggregationService.record(new EcosystemTelemetryEvent(
                System.currentTimeMillis(),
                type,
                artifactSeed,
                lineageId,
                niche,
                attributes == null ? Map.of() : Map.copyOf(attributes)
        ));
    }

    public void flush() {
        aggregationService.flush();
    }

    public ScheduledEcosystemRollups rollups() {
        return aggregationService.rollups();
    }
}
