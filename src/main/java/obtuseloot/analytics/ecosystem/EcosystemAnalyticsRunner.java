package obtuseloot.analytics.ecosystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EcosystemAnalyticsRunner {
    private final AnalyticsJobOrchestrator jobOrchestrator;
    private final EcosystemAnalyticsOrchestrator analyticsOrchestrator;
    private final AnalysisJobPersistence persistence;

    public EcosystemAnalyticsRunner() {
        this(new AnalyticsJobOrchestrator(), new EcosystemAnalyticsOrchestrator(), new AnalysisJobPersistence());
    }

    public EcosystemAnalyticsRunner(AnalyticsJobOrchestrator jobOrchestrator,
                                    EcosystemAnalyticsOrchestrator analyticsOrchestrator,
                                    AnalysisJobPersistence persistence) {
        this.jobOrchestrator = jobOrchestrator;
        this.analyticsOrchestrator = analyticsOrchestrator;
        this.persistence = persistence;
    }

    public AnalyticsOutputBundle run(EcosystemAnalysisJob job) {
        long started = System.currentTimeMillis();
        Path outputDir = job.outputDirectory();
        Path runMetadataPath = null;
        try {
            AnalysisPipelineContext context = jobOrchestrator.prepare(job);
            EcosystemAnalyticsReport report = analyticsOrchestrator.analyze(
                    context.telemetryEvents(), context.selectedWindow(), context.rollupHistory());

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

            Path reportPath = outputDir.resolve(job.jobId() + "-analysis-report.txt");
            Path historyPath = outputDir.resolve("recommendation-history.log");
            RecommendationHistoryStore store = new RecommendationHistoryStore(historyPath);

            Optional<String> diff = store.compareAgainstLatest(record);
            TuningRecommendationRecord persisted = store.append(record.withDecision(RecommendationDecision.PROPOSED,
                    diff.orElse("baseline comparison unavailable")));

            Path exported = null;
            writeReport(reportPath, context, report, persisted);

            AnalysisJobRecord jobRecord = new AnalysisJobRecord(
                    job.jobId(),
                    job.datasetRoot() == null ? outputDir : job.datasetRoot(),
                    outputDir,
                    started,
                    job.bucketPolicy(),
                    context.scenarioMetadata().isEmpty() ? "offline" : "harness",
                    String.valueOf(obtuseloot.telemetry.TelemetryRollupSnapshot.CURRENT_VERSION),
                    context.scenarioMetadata());

            Path jobRecordPath = persistence.writeJobRecord(jobRecord);
            runMetadataPath = persistence.writeRunMetadata(outputDir, new AnalysisRunMetadata(
                    job.jobId(),
                    started,
                    System.currentTimeMillis(),
                    "SUCCESS",
                    jobRecord.sourceKind(),
                    job.bucketPolicy() == null ? "SCENARIO" : job.bucketPolicy().bucketType().name(),
                    context.rollupHistory().size(),
                    context.selectedWindow().size(),
                    context.telemetryEvents().size(),
                    null));

            Map<String, String> artifacts = new LinkedHashMap<>();
            artifacts.put("analysisReport", String.valueOf(reportPath));
            artifacts.put("recommendationHistory", String.valueOf(historyPath));
            artifacts.put("jobRecord", String.valueOf(jobRecordPath));
            artifacts.put("runMetadata", String.valueOf(runMetadataPath));
            if (exported != null) {
                artifacts.put("exportedProfile", String.valueOf(exported));
            }
            Path outputManifestPath = persistence.writeOutputManifest(outputDir, new JobOutputManifest(
                    job.jobId(),
                    Map.copyOf(artifacts),
                    persisted.recommendationId(),
                    persisted.decision().name(),
                    persisted.recommendation().rationale(),
                    report.longTermEvolutionReport().summary()));

            return new AnalyticsOutputBundle(job.jobId(), report, persisted, reportPath, historyPath, exported,
                    jobRecordPath, runMetadataPath, outputManifestPath, context.scenarioMetadata());
        } catch (RuntimeException ex) {
            if (outputDir != null) {
                runMetadataPath = persistence.writeRunMetadata(outputDir, new AnalysisRunMetadata(
                        job.jobId(), started, System.currentTimeMillis(), "FAILED", "offline",
                        job.bucketPolicy() == null ? "SCENARIO" : job.bucketPolicy().bucketType().name(),
                        0, 0, 0, ex.getMessage()));
            }
            throw ex;
        }
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
                    + "decision=" + recommendation.decision() + "\n"
                    + "branch_survival_half_life=" + String.format(Locale.ROOT, "%.3f", report.branchSurvivalHalfLifeReport().branchSurvivalHalfLife()) + "\n"
                    + "cohorts_measured=" + report.branchSurvivalHalfLifeReport().cohortsMeasured() + "\n"
                    + "branch_survival_half_life_censored=" + report.branchSurvivalHalfLifeReport().censoredCohorts() + "\n"
                    + "estimate_status=" + report.branchSurvivalHalfLifeReport().estimateStatus() + "\n"
                    + "branch_survival_half_life_censored_or_complete=" + report.branchSurvivalHalfLifeReport().estimateStatus() + "\n"
                    + "long_term_summary=" + report.longTermEvolutionReport().summary() + "\n";
            Files.writeString(reportPath, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to persist analytics report", ex);
        }
    }

}
