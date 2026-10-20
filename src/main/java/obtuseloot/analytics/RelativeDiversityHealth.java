package obtuseloot.analytics;

import java.util.List;
import java.util.Map;

/** A relative regression signal; it never prescribes a template or niche distribution. */
public final class RelativeDiversityHealth {
    private RelativeDiversityHealth() {}

    public record Assessment(double baselineDiversity, double lowestLaterDiversity,
                             double largestRelativeDrop, boolean healthy) {}

    public static Assessment assess(List<? extends Map<String, ? extends Number>> windows,
                                    int baselineWindows, double maximumRelativeDrop) {
        if (baselineWindows < 1 || windows.size() <= baselineWindows) {
            throw new IllegalArgumentException("Need baseline windows and at least one later window");
        }
        if (!Double.isFinite(maximumRelativeDrop) || maximumRelativeDrop < 0.0D || maximumRelativeDrop > 1.0D) {
            throw new IllegalArgumentException("Relative drop limit must be in [0,1]");
        }
        double baseline = windows.subList(0, baselineWindows).stream()
                .mapToDouble(window -> NicheDiversityMetrics.diversity(window).simpsonDiversity())
                .average().orElseThrow();
        // A zero baseline cannot demonstrate preservation of diversity. Do not silently pass it.
        if (baseline == 0.0D) throw new IllegalArgumentException("No early-run diversity to compare");
        double lowestLater = windows.subList(baselineWindows, windows.size()).stream()
                .mapToDouble(window -> NicheDiversityMetrics.diversity(window).simpsonDiversity())
                .min().orElseThrow();
        double drop = Math.max(0.0D, (baseline - lowestLater) / baseline);
        return new Assessment(baseline, lowestLater, drop, drop <= maximumRelativeDrop);
    }
}
