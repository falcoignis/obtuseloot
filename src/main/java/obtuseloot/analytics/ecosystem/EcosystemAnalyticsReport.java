package obtuseloot.analytics.ecosystem;

public record EcosystemAnalyticsReport(
        NicheEvolutionReport nicheEvolutionReport,
        LineageSuccessReport lineageSuccessReport,
        EcosystemAnomalyReport anomalyReport,
        TuningProfileRecommendation tuningProfileRecommendation,
        LongTermEvolutionReport longTermEvolutionReport
) {
}
