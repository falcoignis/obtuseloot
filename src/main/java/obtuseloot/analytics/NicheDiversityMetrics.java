package obtuseloot.analytics;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Shared measures of current occupancy, never cumulative event or historical niche counts. */
public final class NicheDiversityMetrics {
    private NicheDiversityMetrics() {}

    public record Diversity(int liveNiches, double simpsonDiversity, double effectiveNiches) {}
    public record Transition(int births, int extinctions, double turnover, double populationShift) {}

    /** Simpson diversity is in [0,1]; Shannon effective niches is in [1,liveNiches], or 0 if empty. */
    public static Diversity diversity(Map<String, ? extends Number> occupancy) {
        Map<String, Double> live = liveOccupancy(occupancy);
        double total = live.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0.0D) return new Diversity(0, 0.0D, 0.0D);
        double squares = 0.0D;
        double entropy = 0.0D;
        for (double count : live.values()) {
            double share = count / total;
            squares += share * share;
            entropy -= share * Math.log(share);
        }
        return new Diversity(live.size(), unit(1.0D - squares),
                Math.max(1.0D, Math.min(live.size(), Math.exp(entropy))));
    }

    /** Births/extinctions are changes in occupied niche membership between these two windows. */
    public static Transition transition(Map<String, ? extends Number> previous,
                                        Map<String, ? extends Number> current) {
        Map<String, Double> before = liveOccupancy(previous);
        Map<String, Double> after = liveOccupancy(current);
        var union = new TreeSet<>(before.keySet());
        union.addAll(after.keySet());
        int births = 0;
        int extinctions = 0;
        double beforeTotal = before.values().stream().mapToDouble(Double::doubleValue).sum();
        double afterTotal = after.values().stream().mapToDouble(Double::doubleValue).sum();
        double shift = 0.0D;
        for (String niche : union) {
            if (!before.containsKey(niche)) births++;
            if (!after.containsKey(niche)) extinctions++;
            double p = beforeTotal == 0.0D ? 0.0D : before.getOrDefault(niche, 0.0D) / beforeTotal;
            double q = afterTotal == 0.0D ? 0.0D : after.getOrDefault(niche, 0.0D) / afterTotal;
            shift += Math.abs(q - p);
        }
        double turnover = union.isEmpty() ? 0.0D : (births + extinctions) / (double) union.size();
        // Treat disappearance of the entire population as complete change, not half a distribution.
        double populationShift = before.isEmpty() != after.isEmpty() ? 1.0D : unit(shift * 0.5D);
        return new Transition(births, extinctions, turnover, populationShift);
    }

    private static Map<String, Double> liveOccupancy(Map<String, ? extends Number> occupancy) {
        var live = new TreeMap<String, Double>();
        if (occupancy != null) occupancy.forEach((niche, count) -> {
            double value = count.doubleValue();
            if (!Double.isFinite(value) || value < 0.0D) {
                throw new IllegalArgumentException("Niche occupancy must be finite and non-negative");
            }
            if (value > 0.0D) live.put(niche, value);
        });
        return live;
    }

    private static double unit(double value) { return Math.max(0.0D, Math.min(1.0D, value)); }
}
