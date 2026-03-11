package obtuseloot.abilities;

public record LatentActivationContext(
        double environmentalExposure,
        double interferenceSignal,
        double experienceSignal,
        double lineageDriftSignal,
        double repeatedNicheExposure
) {
    public static LatentActivationContext bounded(double environmentalExposure,
                                                  double interferenceSignal,
                                                  double experienceSignal,
                                                  double lineageDriftSignal,
                                                  double repeatedNicheExposure) {
        return new LatentActivationContext(
                clamp01(environmentalExposure),
                clamp01(interferenceSignal),
                clamp01(experienceSignal),
                clamp01(lineageDriftSignal),
                clamp01(repeatedNicheExposure));
    }

    private static double clamp01(double v) {
        return Math.max(0.0D, Math.min(1.0D, v));
    }
}
