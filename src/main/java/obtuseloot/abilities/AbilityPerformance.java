package obtuseloot.abilities;

/** Expressed effect multipliers; independent of selection weights and template identity. */
public record AbilityPerformance(double outputMultiplier, double recoveryMultiplier) {
    public static final AbilityPerformance FIXED = new AbilityPerformance(1, 1);
    public AbilityPerformance {
        if (!Double.isFinite(outputMultiplier) || outputMultiplier <= 0
                || !Double.isFinite(recoveryMultiplier) || recoveryMultiplier <= 0)
            throw new IllegalArgumentException("Invalid expressed ability performance");
    }
    public double output(double baseline) {
        double value = baseline * outputMultiplier;
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("Invalid expressed output");
        return value;
    }
    public long recoveryTicks(long baseline) {
        double value = Math.ceil(baseline * recoveryMultiplier);
        if (baseline < 0 || !Double.isFinite(value) || value >= Long.MAX_VALUE)
            throw new IllegalArgumentException("Invalid expressed recovery");
        return (long) value;
    }
}
