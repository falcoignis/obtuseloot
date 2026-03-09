package obtuseloot.ecosystem;

import java.util.LinkedHashMap;
import java.util.Map;

public class ArtifactEcosystemBalancer {
    public Map<String, Double> computeAdjustments(Map<String, Integer> familyDistribution) {
        Map<String, Double> out = new LinkedHashMap<>();
        int total = familyDistribution.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return out;
        }
        familyDistribution.forEach((family, count) -> {
            double share = count / (double) total;
            if (share > 0.35D) {
                out.put(family, -0.01D);
            } else if (share < 0.10D) {
                out.put(family, 0.01D);
            } else {
                out.put(family, 0.0D);
            }
        });
        return out;
    }
}
