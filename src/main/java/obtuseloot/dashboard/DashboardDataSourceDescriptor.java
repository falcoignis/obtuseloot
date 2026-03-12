package obtuseloot.dashboard;

public record DashboardDataSourceDescriptor(
        String id,
        String path,
        DashboardSourceKind sourceKind,
        boolean authoritative,
        String notes
) {
    public enum DashboardSourceKind {
        RUNTIME_TELEMETRY_ROLLUP,
        SIMULATION_SNAPSHOT,
        DERIVED_ANALYTIC,
        REPORT_TEXT
    }
}
