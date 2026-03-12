package obtuseloot.analytics;

import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.UtilityHistoryRollup;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.LineageBiasDimension;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class LineageInheritanceAnalytics {

    public Map<String, Object> summarize(Collection<Artifact> artifacts, Collection<ArtifactLineage> lineages) {
        Map<String, Object> summary = new LinkedHashMap<>();
        Map<String, Double> utilityDensityByLineage = new LinkedHashMap<>();
        Map<String, Integer> branchCounts = new LinkedHashMap<>();
        Map<String, Double> weirdnessByLineage = new LinkedHashMap<>();
        Map<String, Integer> descendantsObserved = new LinkedHashMap<>();
        Map<String, Integer> branchBirths = new LinkedHashMap<>();
        Map<String, Integer> branchCollapses = new LinkedHashMap<>();
        Map<String, Integer> branchSurvivors = new LinkedHashMap<>();
        Map<String, Integer> driftWindowRemaining = new LinkedHashMap<>();
        Map<String, Double> specializationCurrent = new LinkedHashMap<>();
        Map<String, Double> specializationTrend = new LinkedHashMap<>();
        Map<String, Double> adaptationVsEcology = new LinkedHashMap<>();
        Map<String, String> lineageState = new LinkedHashMap<>();

        for (ArtifactLineage lineage : lineages) {
            branchCounts.put(lineage.lineageId(), lineage.branches().size());
            weirdnessByLineage.put(lineage.lineageId(), lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.WEIRDNESS));
            descendantsObserved.put(lineage.lineageId(), lineage.descendantsObserved());
            branchBirths.put(lineage.lineageId(), lineage.branchBirths());
            branchCollapses.put(lineage.lineageId(), lineage.branchCollapses());
            branchSurvivors.put(lineage.lineageId(), lineage.branchSurvivors());
            driftWindowRemaining.put(lineage.lineageId(), lineage.driftWindowTicks());
            specializationCurrent.put(lineage.lineageId(), lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.SPECIALIZATION));
            specializationTrend.put(lineage.lineageId(), trend(lineage.specializationTrajectory()));
            adaptationVsEcology.put(lineage.lineageId(), adaptationDelta(lineage.specializationTrajectory(), lineage.ecologicalPressureHistory()));
            lineageState.put(lineage.lineageId(), lifecycleState(lineage));
        }

        Map<String, double[]> utilityAcc = new LinkedHashMap<>();
        Map<String, Integer> lineagePopulation = new LinkedHashMap<>();
        Map<String, Integer> nicheDistribution = new LinkedHashMap<>();
        Map<String, Integer> nichePopulation = new LinkedHashMap<>();
        Map<String, Integer> lineageByNicheCount = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            UtilityHistoryRollup rollup = UtilityHistoryRollup.parse(artifact.getLastUtilityHistory());
            double[] acc = utilityAcc.computeIfAbsent(artifact.getLatentLineage(), ignored -> new double[2]);
            acc[0] += rollup.utilityDensity();
            acc[1] += 1;
            String lineageId = normalize(artifact.getLatentLineage());
            lineagePopulation.merge(lineageId, 1, Integer::sum);
            String niche = artifact.getEvolutionPath() == null || artifact.getEvolutionPath().isBlank()
                    ? "unknown"
                    : artifact.getEvolutionPath();
            nicheDistribution.merge(lineageId + "::" + niche, 1, Integer::sum);
            nichePopulation.merge(niche, 1, Integer::sum);
            lineageByNicheCount.putIfAbsent(lineageId + "::" + niche, 0);
        }
        utilityAcc.forEach((lineage, acc) -> utilityDensityByLineage.put(lineage, acc[1] == 0 ? 0.0D : acc[0] / acc[1]));
        Map<String, Integer> lineageNicheDistribution = new LinkedHashMap<>();
        Map<String, Double> lineageNicheCrowding = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : nicheDistribution.entrySet()) {
            String[] parts = entry.getKey().split("::", 2);
            if (parts.length != 2) {
                continue;
            }
            lineageNicheDistribution.put(entry.getKey(), entry.getValue());
            int totalForNiche = Math.max(1, nichePopulation.getOrDefault(parts[1], 1));
            lineageNicheCrowding.put(entry.getKey(), entry.getValue() / (double) totalForNiche);
        }

        Map<String, Double> branchSurvivalRates = new LinkedHashMap<>();
        for (ArtifactLineage lineage : lineages) {
            int births = Math.max(1, lineage.branchBirths());
            branchSurvivalRates.put(lineage.lineageId(), lineage.branchSurvivors() / (double) births);
        }

        LineagePopulationMetrics populationMetrics = new LineagePopulationMetrics(lineagePopulation, lineageNicheDistribution, lineageNicheCrowding);
        LineageSpecializationTracker specializationTracker = new LineageSpecializationTracker(specializationCurrent, specializationTrend, adaptationVsEcology);
        LineageLifecycleStats lifecycleStats = new LineageLifecycleStats(descendantsObserved, branchBirths, branchCollapses, branchSurvivors, lineageState);
        BranchEvolutionMetrics branchEvolutionMetrics = new BranchEvolutionMetrics(branchCounts, branchSurvivalRates, driftWindowRemaining);

        summary.put("lineageUtilityDensity", utilityDensityByLineage);
        summary.put("lineageUtilityDensityOverTime", lineages.stream().collect(java.util.stream.Collectors.toMap(
                ArtifactLineage::lineageId,
                ArtifactLineage::utilityDensityHistory,
                (a, b) -> b,
                LinkedHashMap::new)));
        summary.put("lineagePopulation", populationMetrics.lineagePopulation());
        summary.put("lineageNicheDistribution", populationMetrics.lineageNicheDistribution());
        summary.put("lineageNicheCrowding", populationMetrics.lineageNicheCrowding());
        summary.put("lineageBranchCounts", branchEvolutionMetrics.branchCounts());
        summary.put("lineageBranchSurvivalRates", branchEvolutionMetrics.branchSurvivalRates());
        summary.put("lineageDriftWindowRemaining", branchEvolutionMetrics.driftWindowRemaining());
        summary.put("lineageSpecializationCurrent", specializationTracker.specializationCurrent());
        summary.put("lineageSpecializationTrend", specializationTracker.specializationTrend());
        summary.put("lineageAdaptationVsEcology", specializationTracker.adaptationVsEcology());
        summary.put("lineageLifecycleDescendants", lifecycleStats.descendantsObserved());
        summary.put("lineageBranchBirths", lifecycleStats.branchBirths());
        summary.put("lineageBranchCollapses", lifecycleStats.branchCollapses());
        summary.put("lineageBranchSurvivors", lifecycleStats.branchSurvivors());
        summary.put("lineageLifecycleState", lifecycleStats.lineageState());
        summary.put("lineageWeirdness", weirdnessByLineage);
        summary.put("branchingLineages", branchCounts.values().stream().filter(count -> count > 0).count());
        return summary;
    }

    private String normalize(String lineage) {
        if (lineage == null || lineage.isBlank()) {
            return "unknown";
        }
        return lineage;
    }

    private double trend(java.util.List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0D;
        }
        return values.get(values.size() - 1) - values.get(0);
    }

    private double adaptationDelta(java.util.List<Double> specialization, java.util.List<Double> ecologyPressure) {
        if (specialization == null || specialization.isEmpty() || ecologyPressure == null || ecologyPressure.isEmpty()) {
            return 0.0D;
        }
        double specTrend = trend(specialization);
        double ecoTrend = trend(ecologyPressure);
        return specTrend - (ecoTrend * 0.30D);
    }

    private String lifecycleState(ArtifactLineage lineage) {
        if (lineage.branchSurvivors() > 0) {
            return "thriving";
        }
        if (lineage.branchBirths() > 0 && lineage.branchCollapses() >= lineage.branchBirths()) {
            return "collapse-risk";
        }
        return lineage.descendantsObserved() > 0 ? "active" : "dormant";
    }
}
