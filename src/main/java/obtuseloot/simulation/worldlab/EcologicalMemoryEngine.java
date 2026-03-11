package obtuseloot.simulation.worldlab;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EcologicalMemoryEngine {
    public record MemoryFeedback(boolean active,
                                 double pressure,
                                 double modifier,
                                 double latentBias,
                                 double mutationBias,
                                 int attractorDuration,
                                 String dominantNiche,
                                 double persistentDominance) {}

    private static final int WINDOW_SIZE = 4;
    private static final double TARGET_DOMINANCE = 0.52D;
    private static final double ALPHA = 0.45D;

    private final Deque<Double> dominantBranchWindow = new ArrayDeque<>();
    private final Deque<Double> dominantNicheWindow = new ArrayDeque<>();
    private final Deque<Double> dominantSpeciesWindow = new ArrayDeque<>();
    private final Deque<Double> dominantTriggerWindow = new ArrayDeque<>();
    private final Deque<Double> dominantMechanicWindow = new ArrayDeque<>();
    private final Deque<Double> dominantEnvironmentWindow = new ArrayDeque<>();
    private final List<Double> pressureTimeline = new ArrayList<>();
    private final List<Double> attractorDurationTimeline = new ArrayList<>();
    private String dominantNiche = "none";
    private String dominantSpecies = "none";
    private String dominantBranch = "none";
    private String dominantTrigger = "none";
    private String dominantMechanic = "none";
    private String dominantEnvironment = "none";
    private int currentAttractorDuration = 0;

    public void observeSeason(Map<String, Integer> branchCounts,
                              Map<String, Integer> nicheOccupancy,
                              Map<String, Integer> speciesCounts,
                              Map<String, Integer> triggerCounts,
                              Map<String, Integer> mechanicCounts,
                              Map<String, Integer> environmentCounts) {
        push(dominantBranchWindow, dominantShare(branchCounts));
        push(dominantNicheWindow, dominantShare(nicheOccupancy));
        push(dominantSpeciesWindow, dominantShare(speciesCounts));
        push(dominantTriggerWindow, dominantShare(triggerCounts));
        push(dominantMechanicWindow, dominantShare(mechanicCounts));
        push(dominantEnvironmentWindow, dominantShare(environmentCounts));

        dominantBranch = dominantToken(branchCounts);
        dominantSpecies = dominantToken(speciesCounts);
        dominantTrigger = dominantToken(triggerCounts);
        dominantMechanic = dominantToken(mechanicCounts);
        dominantEnvironment = dominantToken(environmentCounts);

        String newDominantNiche = dominantToken(nicheOccupancy);
        if (newDominantNiche.equals(dominantNiche)) {
            currentAttractorDuration++;
        } else {
            dominantNiche = newDominantNiche;
            currentAttractorDuration = 1;
        }

        MemoryFeedback feedback = feedback();
        pressureTimeline.add(feedback.pressure());
        attractorDurationTimeline.add((double) currentAttractorDuration);
    }

    public MemoryFeedback feedbackForArtifact(String speciesId,
                                              String nicheId,
                                              String branch,
                                              String triggerProfile,
                                              String mechanicProfile,
                                              String environmentSignature) {
        MemoryFeedback global = feedback();
        if (!global.active()) {
            return global;
        }
        int matches = 0;
        if (speciesId != null && speciesId.equals(dominantSpecies)) matches++;
        if (nicheId != null && nicheId.equals(dominantNiche)) matches++;
        if (branch != null && branch.equals(dominantBranch)) matches++;
        if (containsToken(triggerProfile, dominantTrigger)) matches++;
        if (containsToken(mechanicProfile, dominantMechanic)) matches++;
        if (environmentSignature != null && environmentSignature.equals(dominantEnvironment)) matches++;

        double matchFactor = Math.min(1.0D, matches / 4.0D);
        double modifier = clamp(1.0D - (global.pressure() * 0.10D * matchFactor), 0.90D, 1.0D);
        double latentBias = clamp(1.0D + (global.pressure() * 0.06D * (1.0D - matchFactor)), 1.0D, 1.06D);
        double mutationBias = clamp(1.0D + (global.pressure() * 0.10D * (1.0D - matchFactor)), 1.0D, 1.08D);
        return new MemoryFeedback(true, global.pressure(), modifier, latentBias, mutationBias,
                global.attractorDuration(), global.dominantNiche(), global.persistentDominance());
    }

    public MemoryFeedback feedback() {
        double persistentDominance = average(List.of(
                average(dominantBranchWindow),
                average(dominantNicheWindow),
                average(dominantSpeciesWindow),
                average(dominantTriggerWindow),
                average(dominantMechanicWindow),
                average(dominantEnvironmentWindow)
        ));
        boolean mature = dominantNicheWindow.size() >= WINDOW_SIZE;
        double pressure = mature ? Math.max(0.0D, persistentDominance - TARGET_DOMINANCE) : 0.0D;
        double modifier = clamp(1.0D - (ALPHA * pressure), 0.90D, 1.0D);
        return new MemoryFeedback(pressure > 0.0001D, pressure, modifier, modifier, 1.0D + (pressure * 0.08D),
                currentAttractorDuration, dominantNiche, persistentDominance);
    }

    public Map<String, Object> diagnostics() {
        MemoryFeedback feedback = feedback();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("active", feedback.active());
        out.put("windowSize", WINDOW_SIZE);
        out.put("persistentDominance", feedback.persistentDominance());
        out.put("targetDominance", TARGET_DOMINANCE);
        out.put("memoryPressure", feedback.pressure());
        out.put("attractorDuration", feedback.attractorDuration());
        out.put("dominantNiche", feedback.dominantNiche());
        out.put("pressureTimeline", pressureTimeline);
        out.put("attractorDurationTimeline", attractorDurationTimeline);
        return out;
    }

    private boolean containsToken(String csv, String token) {
        if (csv == null || csv.isBlank() || token == null || token.isBlank() || "none".equals(token)) {
            return false;
        }
        for (String part : csv.split(",")) {
            if (part.trim().equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    private void push(Deque<Double> window, double value) {
        window.addLast(value);
        while (window.size() > WINDOW_SIZE) {
            window.removeFirst();
        }
    }

    private double average(Deque<Double> values) {
        return values.isEmpty() ? 0.0D : values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
    }

    private double average(List<Double> values) {
        return values.isEmpty() ? 0.0D : values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
    }

    private double dominantShare(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return 0.0D;
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return 0.0D;
        }
        int dominant = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return dominant / (double) total;
    }

    private String dominantToken(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return "none";
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("none");
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
