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
            recommendations.add(new BalanceRecommendation("mutation-impact", "More than half of artifacts remain unmutated", "Slightly increase mutation chance under memory pressure and drift >= 2.", "medium"));
        }
        return new EcosystemHealthReport(families, branches, mutations, triggers, mechanics, memories, recommendations);
    }

    private void recommendUnderrepresented(String category, Map<String, Integer> distribution, List<BalanceRecommendation> out) {
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0 || distribution.isEmpty()) return;
        var low = distribution.entrySet().stream().min(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);
        var high = distribution.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);
        if (low != null && high != null && low.getValue() < Math.max(1, high.getValue() / 4)) {
            out.add(new BalanceRecommendation(category + "-diversity", "Low representation: " + low.getKey() + "=" + low.getValue() + " vs " + high.getKey() + "=" + high.getValue(),
                    "Increase selection weight for " + low.getKey() + " templates/branches by 5-10% until parity improves.", "low"));
        }
    }

    private void recommendDeadWeightMemory(Map<String, Integer> memories, List<BalanceRecommendation> out) {
        memories.forEach((k, v) -> {
            if (v == 0) {
                out.add(new BalanceRecommendation("memory-influence", "Event " + k + " never influenced generated profiles", "Route this memory event into family and branch scoring for at least one family.", "low"));
            }
        });
    }
}
