package obtuseloot.telemetry;

public class TelemetryAggregationAnalytics {
    private final ScheduledEcosystemRollups rollups;

    public TelemetryAggregationAnalytics(ScheduledEcosystemRollups rollups) {
        this.rollups = rollups;
    }

    public NichePopulationRollup nichePopulationRollup() {
        return rollups.nichePopulationRollup();
    }

    public LineagePopulationRollup lineagePopulationRollup() {
        return rollups.lineagePopulationRollup();
    }

    public EcosystemSnapshot ecosystemSnapshot() {
        return rollups.ecosystemSnapshot();
    }
}
