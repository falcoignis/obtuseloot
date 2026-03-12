package obtuseloot.analytics.ecosystem;

public record GovernanceMetadata(
        String sourceAnalysisJobId,
        long generatedAtMs,
        String analysisWindow,
        String sourceKind
) {
}
