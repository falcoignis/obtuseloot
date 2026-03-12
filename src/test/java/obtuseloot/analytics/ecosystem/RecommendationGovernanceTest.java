package obtuseloot.analytics.ecosystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationGovernanceTest {

    @TempDir
    Path tempDir;

    @Test
    void supportsStateTransitionsAndRuntimeCompatibleExport() throws Exception {
        RecommendationHistoryStore store = new RecommendationHistoryStore(tempDir.resolve("history.log"));
        TuningRecommendationRecord proposed = new TuningRecommendationRecord(
                "rec-1",
                1234L,
                new TuningProfileRecommendation("phase6", Map.of(
                        "niche_saturation_sensitivity", 0.95D,
                        "mutation_amplitude_min", 0.11D,
                        "lineage_momentum_decay", 0.3D), "because"),
                new GovernanceMetadata("job-1", 1234L, "DAILY", "offline"),
                RecommendationDecision.PROPOSED,
                "pending");

        store.append(proposed);
        TuningRecommendationRecord accepted = store.setDecision("rec-1", RecommendationDecision.ACCEPTED, "approved").orElseThrow();
        assertEquals(RecommendationDecision.ACCEPTED, accepted.decision());

        Path profile = new TuningProfileExport().exportAccepted(accepted, tempDir.resolve("profile.properties"));
        String body = Files.readString(profile);
        assertTrue(body.contains("ecosystem.parameters.nicheSaturationSensitivity=0.95"));
        assertTrue(body.contains("ecosystem.parameters.mutationAmplitudeMin=0.11"));
    }
}
