package obtuseloot.ecosystem;

import java.util.LinkedHashMap;
import java.util.Map;

public class EcosystemBiasCalculator {
    public Map<String, Double> calculate(WorldEcosystemProfile profile) {
        Map<String, Double> out = new LinkedHashMap<>();
        // Track ecosystem pressure from world signals.
        double precisionBias = normalized(profile.bossKillRate() * 0.40D
                + (1.0D - profile.combatAggression()) * 0.35D
                + profile.memoryEventDistribution() * 0.25D);
        double chaosBias = normalized(profile.combatAggression() * 0.45D
                + profile.memoryEventDistribution() * 0.35D
                + profile.chaosActivityLevel() * 0.20D);
        double mobilityBias = normalized(profile.mobilityUsage() * 0.70D
                + profile.combatAggression() * 0.20D
                + profile.memoryEventDistribution() * 0.10D);
        double survivalBias = normalized(profile.survivalPressure() * 0.65D
                + (1.0D - profile.bossKillRate()) * 0.20D
                + (1.0D - profile.combatAggression()) * 0.15D);

        out.put("precision", scaled(precisionBias));
        out.put("chaos", scaled(chaosBias));
        out.put("mobility", scaled(mobilityBias));
        out.put("survival", scaled(survivalBias));
        return out;
    }

    private double normalized(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private double scaled(double value) {
        return (value - 0.5D) * 0.30D; // capped to +/- 0.15
    }
}
