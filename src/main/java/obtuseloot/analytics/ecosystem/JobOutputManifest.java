package obtuseloot.analytics.ecosystem;

import java.util.Map;

public record JobOutputManifest(
        String jobId,
        Map<String, String> artifacts,
        String recommendationId,
        String recommendationDecision,
        String recommendationRationale,
        String analysisSummary
) {
}
