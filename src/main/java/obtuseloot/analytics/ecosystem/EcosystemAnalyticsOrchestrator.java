package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.TelemetryRollupSnapshot;

import java.util.List;

public class EcosystemAnalyticsOrchestrator {
    private final EcosystemTrendAnalyzer trendAnalyzer;
    private final LineageSuccessAnalyzer lineageSuccessAnalyzer;
    private final EcosystemAnomalyDetector anomalyDetector;
    private final EcosystemTuningRecommender tuningRecommender;
    private final LongTermEvolutionAnalyzer longTermEvolutionAnalyzer;

    public EcosystemAnalyticsOrchestrator() {
        this(new EcosystemTrendAnalyzer(), new LineageSuccessAnalyzer(), new EcosystemAnomalyDetector(),
                new EcosystemTuningRecommender(), new LongTermEvolutionAnalyzer());
    }

    public EcosystemAnalyticsOrchestrator(EcosystemTrendAnalyzer trendAnalyzer,
                                          LineageSuccessAnalyzer lineageSuccessAnalyzer,
                                          EcosystemAnomalyDetector anomalyDetector,
                                          EcosystemTuningRecommender tuningRecommender,
                                          LongTermEvolutionAnalyzer longTermEvolutionAnalyzer) {
        this.trendAnalyzer = trendAnalyzer;
        this.lineageSuccessAnalyzer = lineageSuccessAnalyzer;
        this.anomalyDetector = anomalyDetector;
        this.tuningRecommender = tuningRecommender;
        this.longTermEvolutionAnalyzer = longTermEvolutionAnalyzer;
    }

    public EcosystemAnalyticsReport analyze(List<TelemetryRollupSnapshot> rollupHistory) {
        NicheEvolutionReport nicheReport = trendAnalyzer.analyze(rollupHistory);
        LineageSuccessReport lineageReport = lineageSuccessAnalyzer.analyze(
                rollupHistory.stream().map(TelemetryRollupSnapshot::ecosystemSnapshot).toList());
        EcosystemAnomalyReport anomaly = anomalyDetector.detect(nicheReport, lineageReport);
        TuningProfileRecommendation tuning = tuningRecommender.recommend(nicheReport, lineageReport, anomaly);
        LongTermEvolutionReport longTerm = longTermEvolutionAnalyzer.analyze(rollupHistory);

        return new EcosystemAnalyticsReport(nicheReport, lineageReport, anomaly, tuning, longTerm);
    }
}
