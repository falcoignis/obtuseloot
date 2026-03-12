package obtuseloot.analytics.ecosystem;

public record TuningRecommendationRecord(
        String recommendationId,
        long generatedAtMs,
        TuningProfileRecommendation recommendation,
        GovernanceMetadata governanceMetadata,
        RecommendationDecision decision,
        String decisionNote
) {
    public TuningRecommendationRecord withDecision(RecommendationDecision next, String note) {
        return new TuningRecommendationRecord(recommendationId, generatedAtMs, recommendation, governanceMetadata, next, note);
    }
}
