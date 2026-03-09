package obtuseloot.evolution;

import obtuseloot.artifacts.Artifact;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.reputation.ArtifactReputation;

import java.util.HashMap;
import java.util.Map;

public class ArchetypeResolver {
    public String resolve(Artifact artifact, ArtifactReputation reputation) {
        Map<String, Double> stats = new HashMap<>();
        for (String s : new String[]{"precision", "brutality", "survival", "mobility", "chaos", "consistency"}) {
            stats.put(s, effective(s, artifact, reputation));
        }

        Map<String, Double> scores = Map.of(
                "vanguard", stats.get("survival") + stats.get("consistency"),
                "deadeye", stats.get("precision") + stats.get("consistency"),
                "ravager", stats.get("brutality") + stats.get("chaos"),
                "strider", stats.get("mobility") + stats.get("precision"),
                "harbinger", stats.get("chaos") + stats.get("survival"),
                "warden", stats.get("survival") + stats.get("precision")
        );

        String current = artifact.getArchetypePath();
        double inertia = RuntimeSettings.get().currentArchetypeInertia();
        if (scores.containsKey(current)) {
            scores = new HashMap<>(scores);
            scores.put(current, scores.get(current) + inertia);
        }

        String best = scores.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("vanguard");
        double bestValue = scores.get(best);
        double currentValue = scores.getOrDefault(current, Double.NEGATIVE_INFINITY);
        if (scores.containsKey(current) && bestValue - currentValue < RuntimeSettings.get().archetypeSwitchMargin()) {
            return current;
        }
        return best;
    }

    private double effective(String stat, Artifact artifact, ArtifactReputation reputation) {
        double raw = switch (stat) {
            case "precision" -> reputation.getPrecision();
            case "brutality" -> reputation.getBrutality();
            case "survival" -> reputation.getSurvival();
            case "mobility" -> reputation.getMobility();
            case "chaos" -> reputation.getChaos();
            case "consistency" -> reputation.getConsistency();
            default -> 0;
        };
        return raw + artifact.getSeedAffinity(stat) + artifact.getDriftBias(stat) + artifact.getAwakeningBias(stat);
    }
}
