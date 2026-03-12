package obtuseloot.telemetry;

import java.util.Map;

public class EcosystemTelemetryEmitter {
    private final TelemetryAggregationService aggregationService;
    private final TelemetryEventFactory eventFactory;

    public EcosystemTelemetryEmitter(TelemetryAggregationService aggregationService) {
        this(aggregationService, new TelemetryEventFactory());
    }

    public EcosystemTelemetryEmitter(TelemetryAggregationService aggregationService,
                                     TelemetryEventFactory eventFactory) {
        this.aggregationService = aggregationService;
        this.eventFactory = eventFactory;
    }

    public void emit(EcosystemTelemetryEventType type,
                     long artifactSeed,
                     String lineageId,
                     String niche,
                     Map<String, String> attributes) {
        aggregationService.record(eventFactory.create(type, artifactSeed, lineageId, niche, attributes));
    }

    public void flush() {
        aggregationService.flush();
    }

    public void flushAll() {
        aggregationService.flushAll();
    }

    public void scheduledTick(long nowMs) {
        aggregationService.scheduledRollupTick(nowMs);
    }

    public ScheduledEcosystemRollups rollups() {
        return aggregationService.rollups();
    }
}
