package obtuseloot.analytics.ecosystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class TuningProfileExport {
    private static final Map<String, String> PARAMETER_KEY_MAPPING = Map.of(
            "niche_saturation_sensitivity", "ecosystem.parameters.nicheSaturationSensitivity",
            "lineage_momentum_decay", "ecosystem.parameters.lineageMomentumInfluence",
            "mutation_amplitude_min", "ecosystem.parameters.mutationAmplitudeMin",
            "mutation_amplitude_max", "ecosystem.parameters.mutationAmplitudeMax",
            "competition_reinforcement_scaling", "ecosystem.parameters.competitionReinforcementCurve"
    );

    public Path exportAccepted(TuningRecommendationRecord record, Path outFile) {
        if (record.decision() != RecommendationDecision.ACCEPTED && record.decision() != RecommendationDecision.APPLIED) {
            throw new IllegalArgumentException("Only accepted/applied recommendations can be exported.");
        }
        Properties p = new Properties();
        p.setProperty("metadata.recommendationId", record.recommendationId());
        p.setProperty("metadata.generatedAtMs", String.valueOf(record.generatedAtMs()));
        p.setProperty("metadata.sourceAnalysisJobId", record.governanceMetadata().sourceAnalysisJobId());
        p.setProperty("metadata.analysisWindow", record.governanceMetadata().analysisWindow());
        p.setProperty("metadata.sourceKind", record.governanceMetadata().sourceKind());
        p.setProperty("metadata.rationale", record.recommendation().rationale());

        Map<String, Double> exported = new LinkedHashMap<>();
        record.recommendation().parameterAdjustments().forEach((k, v) -> {
            String runtimeKey = PARAMETER_KEY_MAPPING.get(k);
            if (runtimeKey != null) {
                p.setProperty(runtimeKey, String.valueOf(v));
                exported.put(runtimeKey, v);
            }
        });
        if (exported.isEmpty()) {
            throw new IllegalStateException("No runtime-compatible keys found for recommendation " + record.recommendationId());
        }

        try {
            Files.createDirectories(outFile.getParent());
            StringBuilder sb = new StringBuilder();
            for (String key : p.stringPropertyNames().stream().sorted().toList()) {
                sb.append(key).append('=').append(p.getProperty(key)).append('\n');
            }
            Files.writeString(outFile, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return outFile;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to export tuning profile", ex);
        }
    }

    public Map<String, String> parameterKeyMapping() {
        return PARAMETER_KEY_MAPPING;
    }
}
