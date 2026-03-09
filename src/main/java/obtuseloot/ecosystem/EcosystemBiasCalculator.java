package obtuseloot.ecosystem;

import java.util.LinkedHashMap;
import java.util.Map;

public class EcosystemBiasCalculator {
    public Map<String, Double> calculate(WorldEcosystemProfile profile) {
        Map<String, Double> out = new LinkedHashMap<>();
        double chaos = normalized(profile.chaosActivityLevel() * 0.5D + profile.driftMutationFrequency() * 0.35D + profile.memoryEventDistribution() * 0.15D);
        double precision = normalized(profile.precisionBehavior() * 0.55D + profile.bossKillRate() * 0.25D + profile.memoryEventDistribution() * 0.20D);
        double survival = normalized(profile.survivalPressure() * 0.65D + profile.bossKillRate() * 0.20D + (1.0D - profile.combatAggression()) * 0.15D);
        double mobility = normalized(profile.mobilityUsage() * 0.75D + profile.combatAggression() * 0.10D + profile.memoryEventDistribution() * 0.15D);
        double brutality = normalized(profile.combatAggression() * 0.65D + profile.chaosActivityLevel() * 0.20D + profile.bossKillRate() * 0.15D);
        double consistency = normalized((1.0D - profile.chaosActivityLevel()) * 0.45D + profile.precisionBehavior() * 0.30D + (1.0D - profile.driftMutationFrequency()) * 0.25D);

        out.put("chaos", scaled(chaos));
        out.put("precision", scaled(precision));
        out.put("survival", scaled(survival));
        out.put("mobility", scaled(mobility));
        out.put("brutality", scaled(brutality));
        out.put("consistency", scaled(consistency));
        return out;
    }

    private double normalized(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private double scaled(double value) {
        return (value - 0.5D) * 0.30D; // capped to +/- 0.15
    }
}
