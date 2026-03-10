package obtuseloot.species;

import java.util.LinkedHashMap;
import java.util.Map;

public record ArtifactPopulationSignature(
        Map<String, Double> genomeTraits,
        Map<String, Double> branchPreferences,
        Map<String, Double> triggerProfile,
        Map<String, Double> mechanicProfile,
        Map<String, Double> gateProfile,
        Map<String, Double> environmentalProfile
) {
    public ArtifactPopulationSignature {
        genomeTraits = normalizedCopy(genomeTraits);
        branchPreferences = normalizedCopy(branchPreferences);
        triggerProfile = normalizedCopy(triggerProfile);
        mechanicProfile = normalizedCopy(mechanicProfile);
        gateProfile = normalizedCopy(gateProfile);
        environmentalProfile = normalizedCopy(environmentalProfile);
    }

    private static Map<String, Double> normalizedCopy(Map<String, Double> input) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (input == null) {
            return out;
        }
        input.forEach((k, v) -> {
            if (k != null && !k.isBlank() && v != null && v > 0.0D) {
                out.put(k, v);
            }
        });
        double total = out.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0.0D) {
            return out;
        }
        out.replaceAll((k, v) -> v / total);
        return out;
    }
}
