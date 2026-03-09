package obtuseloot.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EcosystemBalanceAnalyzer {
    public EcosystemHealthReport analyze(Map<String, Integer> families,
                                         Map<String, Integer> branches,
                                         Map<String, Integer> mutations,
                                         Map<String, Integer> triggers,
                                         Map<String, Integer> mechanics,
                                         Map<String, Integer> memories) {
        List<BalanceRecommendation> recommendations = new ArrayList<>();
        recommendUnderrepresented("family", families, recommendations);
        recommendUnderrepresented("branch", branches, recommendations);
        recommendUnderrepresented("trigger", triggers, recommendations);
        recommendUnderrepresented("mechanic", mechanics, recommendations);
        recommendDeadWeightMemory(memories, recommendations);
        if (mutations.getOrDefault("none", 0) > mutations.values().stream().mapToInt(Integer::intValue).sum() * 0.5D) {
            recommendations.add(new BalanceRecommendation(
                    "mutation-impact",
                    "Large no-mutation share",
                    "More than half of artifacts remain unmutated",
                    "moderate",
                    "mid/late season medium",
                    "Slightly increase mutation chance under memory pressure and drift >= 2.",
                    "candidate for threshold adjustment",
                    "medium"
            ));
        }
        return new EcosystemHealthReport(families, branches, mutations, triggers, mechanics, memories, recommendations);
    }

    private void recommendUnderrepresented(String category, Map<String, Integer> distribution, List<BalanceRecommendation> out) {
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0 || distribution.isEmpty()) return;
        var low = distribution.entrySet().stream().min(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);
        var high = distribution.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);
        if (low != null && high != null && low.getValue() < Math.max(1, high.getValue() / 4)) {
            out.add(new BalanceRecommendation(
                    category + "-diversity",
                    "Underrepresented " + category + " candidate",
                    "Low representation: " + low.getKey() + "=" + low.getValue() + " vs " + high.getKey() + "=" + high.getValue(),
                    "moderate",
                    "early/mid season low-medium",
                    "Increase selection weight for " + low.getKey() + " templates/branches by 5-10% until parity improves.",
                    "candidate for small weight adjustment",
                    "low"
            ));
        }
    }

    private void recommendDeadWeightMemory(Map<String, Integer> memories, List<BalanceRecommendation> out) {
        memories.forEach((k, v) -> {
            if (v == 0) {
                out.add(new BalanceRecommendation(
                        "memory-influence",
                        "Dead memory event",
                        "Event " + k + " never influenced generated profiles",
                        "high",
                        "late season medium",
                        "Route this memory event into family and branch scoring for at least one family.",
                        "needs another simulation pass",
                        "low"
                ));
            }
        });
    }
}
