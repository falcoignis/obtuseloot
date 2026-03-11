package obtuseloot.simulation.worldlab;

import java.util.Locale;

public record FitnessSharingConfig(boolean enabled,
                                   String mode,
                                   double alpha,
                                   double maxPenalty,
                                   double targetOccupancy,
                                   double similarityRadius) {
    public static FitnessSharingConfig defaults() {
        return new FitnessSharingConfig(true, "niche", 0.10D, 0.15D, 0.18D, 0.20D);
    }

    public String normalizedMode() {
        if (mode == null) {
            return "niche";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("niche") && !normalized.equals("distance")) {
            return "niche";
        }
        return normalized;
    }

    public FitnessSharingConfig bounded() {
        return new FitnessSharingConfig(
                enabled,
                normalizedMode(),
                clamp(alpha, 0.0D, 0.25D),
                clamp(maxPenalty, 0.0D, 0.20D),
                clamp(targetOccupancy, 0.05D, 0.40D),
                clamp(similarityRadius, 0.05D, 0.50D));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
