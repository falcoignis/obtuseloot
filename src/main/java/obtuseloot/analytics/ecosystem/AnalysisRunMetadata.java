package obtuseloot.analytics.ecosystem;

public record AnalysisRunMetadata(
        String jobId,
        long startedAtMs,
        long finishedAtMs,
        String status,
        String sourceKind,
        String analysisWindow,
        int rollupsLoaded,
        int rollupsSelected,
        int telemetryEventsLoaded,
        String failureReason
) {
}
