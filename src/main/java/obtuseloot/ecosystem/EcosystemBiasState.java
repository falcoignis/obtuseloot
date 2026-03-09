package obtuseloot.ecosystem;

import java.util.LinkedHashMap;
import java.util.Map;

public class EcosystemBiasState {
    public static final double MAX_BIAS = 0.15D;
    private final Map<String, Double> biasByFamily = new LinkedHashMap<>();
    private long updates;

    public EcosystemBiasState() {
        for (String family : new String[]{"precision", "brutality", "survival", "mobility", "chaos", "consistency"}) {
            biasByFamily.put(family, 0.0D);
        }
    }

    public Map<String, Double> biasByFamily() {
        return biasByFamily;
    }

    public double biasFor(String family) {
        return biasByFamily.getOrDefault(family.toLowerCase(), 0.0D);
    }

    public long updates() {
        return updates;
    }

    public void setUpdates(long updates) {
        this.updates = updates;
    }

    public void mergeTarget(Map<String, Double> targetBias) {
        for (Map.Entry<String, Double> entry : targetBias.entrySet()) {
            String key = entry.getKey().toLowerCase();
            double current = biasByFamily.getOrDefault(key, 0.0D);
            double target = clamp(entry.getValue(), -MAX_BIAS, MAX_BIAS);
            double stepped = current + ((target - current) * 0.2D); // slow drift
            biasByFamily.put(key, clamp(stepped, -MAX_BIAS, MAX_BIAS));
        }
        updates++;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
