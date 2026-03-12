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

        for (ArtifactLineage lineage : lineages) {
            branchCounts.put(lineage.lineageId(), lineage.branches().size());
            weirdnessByLineage.put(lineage.lineageId(), lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.WEIRDNESS));
        }

        Map<String, double[]> utilityAcc = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            UtilityHistoryRollup rollup = UtilityHistoryRollup.parse(artifact.getLastUtilityHistory());
            double[] acc = utilityAcc.computeIfAbsent(artifact.getLatentLineage(), ignored -> new double[2]);
            acc[0] += rollup.utilityDensity();
            acc[1] += 1;
        }
        utilityAcc.forEach((lineage, acc) -> utilityDensityByLineage.put(lineage, acc[1] == 0 ? 0.0D : acc[0] / acc[1]));

        summary.put("lineageUtilityDensity", utilityDensityByLineage);
        summary.put("lineageBranchCounts", branchCounts);
        summary.put("lineageWeirdness", weirdnessByLineage);
        summary.put("branchingLineages", branchCounts.values().stream().filter(count -> count > 0).count());
        return summary;
    }
}
