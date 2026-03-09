package obtuseloot.analytics;

public record BalanceRecommendation(
        String category,
        String issueSummary,
        String evidence,
        String confidence,
        String estimatedImpact,
        String suggestion,
        String action,
        String severity
) {
}
