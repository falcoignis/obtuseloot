package obtuseloot.telemetry;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class EcosystemTelemetryEmitter {
    private final TelemetryAggregationService aggregationService;
    private final TelemetryEventFactory eventFactory;
    private final double samplingRate;

    public EcosystemTelemetryEmitter(TelemetryAggregationService aggregationService) {
        this(aggregationService, new TelemetryEventFactory());
    }

    public EcosystemTelemetryEmitter(TelemetryAggregationService aggregationService,
                                     TelemetryEventFactory eventFactory) {
        this(aggregationService, eventFactory, 1.0D);
    }

    public EcosystemTelemetryEmitter(TelemetryAggregationService aggregationService,
                                     TelemetryEventFactory eventFactory,
                                     double samplingRate) {
        this.aggregationService = aggregationService;
        this.eventFactory = eventFactory;
        this.samplingRate = Math.max(0.0D, Math.min(1.0D, samplingRate));
    }

    public void emit(EcosystemTelemetryEventType type,
                     long artifactSeed,
                     String lineageId,
                     String niche,
                     Map<String, String> attributes) {
        if (!shouldEmit(type, attributes)) {
            return;
        }
        aggregationService.record(eventFactory.create(type, artifactSeed, lineageId, niche, attributes));
    }

    private boolean shouldEmit(EcosystemTelemetryEventType type, Map<String, String> attributes) {
        if (samplingRate >= 1.0D || isCriticalEvent(type, attributes)) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < samplingRate;
    }

    private boolean isCriticalEvent(EcosystemTelemetryEventType type, Map<String, String> attributes) {
        if (type == EcosystemTelemetryEventType.ROLLUP_GENERATED
                || type == EcosystemTelemetryEventType.LINEAGE_UPDATE
                || type == EcosystemTelemetryEventType.BRANCH_FORMATION) {
            return true;
        }
        String contextTags = attributes == null ? "" : attributes.getOrDefault("context_tags", "").toLowerCase();
        return contextTags.contains("anomaly") || contextTags.contains("branch-collapse") || contextTags.contains("lifecycle");
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
