package obtuseloot.analytics.ecosystem;

import java.nio.file.Path;
import java.util.Map;

public record AnalyticsOutputBundle(
        String jobId,
        EcosystemAnalyticsReport report,
        TuningRecommendationRecord recommendationRecord,
        Path reportPath,
        Path recommendationHistoryPath,
        Path exportedProfilePath,
        Path jobRecordPath,
        Path runMetadataPath,
        Path outputManifestPath,
        Map<String, String> scenarioMetadata
) {
}
