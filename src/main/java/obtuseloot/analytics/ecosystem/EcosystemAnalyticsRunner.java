package obtuseloot.analytics.ecosystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class EcosystemAnalyticsRunner {
    private final AnalyticsJobOrchestrator jobOrchestrator;
    private final EcosystemAnalyticsOrchestrator analyticsOrchestrator;

    public EcosystemAnalyticsRunner() {
        this(new AnalyticsJobOrchestrator(), new EcosystemAnalyticsOrchestrator());
    }

    public EcosystemAnalyticsRunner(AnalyticsJobOrchestrator jobOrchestrator,
                                    EcosystemAnalyticsOrchestrator analyticsOrchestrator) {
        this.jobOrchestrator = jobOrchestrator;
        this.analyticsOrchestrator = analyticsOrchestrator;
    }

    public AnalyticsOutputBundle run(EcosystemAnalysisJob job) {
        AnalysisPipelineContext context = jobOrchestrator.prepare(job);
        EcosystemAnalyticsReport report = analyticsOrchestrator.analyze(context.selectedWindow(), context.rollupHistory());

        long now = System.currentTimeMillis();
        String recId = job.jobId() + "-" + UUID.randomUUID();
        TuningRecommendationRecord record = new TuningRecommendationRecord(
                recId,
                now,
                report.tuningProfileRecommendation(),
                new GovernanceMetadata(job.jobId(), now,
                        job.bucketPolicy() == null ? "scenario" : job.bucketPolicy().bucketType().name(),
                        context.scenarioMetadata().isEmpty() ? "offline" : "harness"),
                RecommendationDecision.PROPOSED,
                "pending human review");

        Path outputDir = job.outputDirectory();
        Path reportPath = outputDir.resolve(job.jobId() + "-analysis-report.txt");
        Path historyPath = outputDir.resolve("recommendation-history.log");
        RecommendationHistoryStore store = new RecommendationHistoryStore(historyPath);

        Optional<String> diff = store.compareAgainstLatest(record);
        TuningRecommendationRecord persisted = store.append(record.withDecision(RecommendationDecision.PROPOSED,
                diff.orElse("baseline comparison unavailable")));

        Path exported = null;
        writeReport(reportPath, context, report, persisted);

        return new AnalyticsOutputBundle(job.jobId(), report, persisted, reportPath, historyPath, exported, context.scenarioMetadata());
    }

    private void writeReport(Path reportPath,
                             AnalysisPipelineContext context,
                             EcosystemAnalyticsReport report,
                             TuningRecommendationRecord recommendation) {
        try {
            Files.createDirectories(reportPath.getParent());
            String text = "job=" + context.job().jobId() + "\n"
                    + "rollups_loaded=" + context.rollupHistory().size() + "\n"
                    + "window_size=" + context.selectedWindow().size() + "\n"
                    + "telemetry_events=" + context.telemetryEvents().size() + "\n"
                    + "runaway_lineages=" + report.anomalyReport().runawayLineages() + "\n"
                    + "niche_collapse=" + report.anomalyReport().nicheCollapse() + "\n"
                    + "severity=" + String.format(Locale.ROOT, "%.3f", report.anomalyReport().anomalySeverityScore()) + "\n"
                    + "recommendation_id=" + recommendation.recommendationId() + "\n"
                    + "decision=" + recommendation.decision() + "\n";
            Files.writeString(reportPath, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to persist analytics report", ex);
        }
    }
}
