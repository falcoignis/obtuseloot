package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.EcosystemSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LineageSuccessAnalyzer {

    public LineageSuccessReport analyze(List<EcosystemSnapshot> history) {
        if (history == null || history.size() < 2) {
            return new LineageSuccessReport(Map.of(), Map.of(), Map.of(), List.of(), List.of());
        }

        EcosystemSnapshot first = history.getFirst();
        EcosystemSnapshot last = history.getLast();
        Map<String, Long> firstPopulation = first.lineagePopulationRollup().populationByLineage();
        Map<String, Long> lastPopulation = last.lineagePopulationRollup().populationByLineage();
        Map<String, Long> firstBranches = first.lineagePopulationRollup().branchCountByLineage();
        Map<String, Long> lastBranches = last.lineagePopulationRollup().branchCountByLineage();
        Map<String, Double> firstSpecialization = first.lineagePopulationRollup().specializationTrajectoryByLineage();
        Map<String, Double> lastSpecialization = last.lineagePopulationRollup().specializationTrajectoryByLineage();

        Map<String, Double> successRates = new LinkedHashMap<>();
        Map<String, Double> branchSurvival = new LinkedHashMap<>();
        Map<String, Double> specializationCascadeRisk = new LinkedHashMap<>();
        List<String> collapsing = new ArrayList<>();
        List<String> runaway = new ArrayList<>();

        for (String lineage : lastPopulation.keySet()) {
            long start = Math.max(1L, firstPopulation.getOrDefault(lineage, 0L));
            long end = Math.max(0L, lastPopulation.getOrDefault(lineage, 0L));
            double rate = ((double) end) / start;
            successRates.put(lineage, rate);
            if (rate < 0.6D) {
                collapsing.add(lineage);
            } else if (rate > 2.5D) {
                runaway.add(lineage);
            }

            long startBranches = Math.max(1L, firstBranches.getOrDefault(lineage, 0L));
            long endBranches = Math.max(0L, lastBranches.getOrDefault(lineage, 0L));
            branchSurvival.put(lineage, ((double) endBranches) / startBranches);

            double specializationShift = lastSpecialization.getOrDefault(lineage, 0.0D)
                    - firstSpecialization.getOrDefault(lineage, 0.0D);
            specializationCascadeRisk.put(lineage, Math.abs(specializationShift));
        }

        return new LineageSuccessReport(Map.copyOf(successRates), Map.copyOf(branchSurvival),
                Map.copyOf(specializationCascadeRisk), List.copyOf(collapsing), List.copyOf(runaway));
    }
}
