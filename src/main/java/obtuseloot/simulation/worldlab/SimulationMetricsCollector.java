package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityProfile;
import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryEvent;

import java.util.*;

public class SimulationMetricsCollector {
    private final Map<String, Integer> seeds = new HashMap<>();
    private final Map<String, Integer> archetypes = new HashMap<>();
    private final Map<String, Integer> stages = new HashMap<>();
    private final Map<String, Integer> awakenings = new HashMap<>();
    private final Map<String, Integer> fusions = new HashMap<>();
    private final Map<String, Integer> driftAlignments = new HashMap<>();
    private final Map<String, Integer> branchPaths = new HashMap<>();
    private final Map<String, Integer> memoryProfiles = new HashMap<>();
    private final Map<String, Integer> abilityFamilies = new HashMap<>();
    private final Map<String, Integer> triggers = new HashMap<>();
    private final Map<String, Integer> mechanics = new HashMap<>();
    private final Map<String, Integer> mutationCounts = new HashMap<>();
    private final Map<String, Integer> playstyleClusters = new HashMap<>();
    private final Map<String, Integer> sessionLengths = new HashMap<>();
    private final List<Double> diversityTimeline = new ArrayList<>();
    private final List<Double> dominantFamilyTimeline = new ArrayList<>();
    private final Map<String, Integer> lineageCounts = new HashMap<>();
    private final Map<String, Integer> lineageDepth = new HashMap<>();
    private int lineageExtinctions;

    private int bossEncounters;
    private int lowHealthSurvivals;
    private int lowHealthMoments;
    private int chainMoments;
    private int sessions;

    public void recordArtifact(Artifact artifact) {
        bump(seeds, Long.toUnsignedString(artifact.getArtifactSeed() & 1023));
        bump(archetypes, artifact.getArchetypePath());
        bump(stages, artifact.getEvolutionPath());
        bump(awakenings, artifact.getAwakeningPath());
        bump(fusions, artifact.getFusionPath());
        bump(driftAlignments, artifact.getDriftAlignment());
        bump(branchPaths, artifact.getLastAbilityBranchPath());
        bump(mutationCounts, artifact.getLastMutationHistory());
        bump(lineageCounts, artifact.getLatentLineage());
        lineageDepth.merge(artifact.getLatentLineage(), 1, Integer::sum);
        for (ArtifactMemoryEvent event : ArtifactMemoryEvent.values()) {
            if (artifact.getMemory().count(event) > 0) {
                bump(memoryProfiles, event.name().toLowerCase());
            }
        }
    }

    public void recordAbilityProfile(AbilityProfile profile) {
        for (AbilityDefinition definition : profile.abilities()) {
            bump(abilityFamilies, definition.family().name().toLowerCase());
            bump(triggers, definition.trigger().name().toLowerCase());
            bump(mechanics, definition.mechanic().name().toLowerCase());
        }
    }

    public void recordPlayerProfile(SimulatedPlayer.BehaviorProfile profile) {
        String cluster = profile.aggression() + profile.chaos() > 1.1D ? "aggressive-chaotic" :
                profile.precision() + profile.consistency() > 1.2D ? "disciplined" :
                        profile.survival() > 0.7D ? "survivor" : "balanced";
        bump(playstyleClusters, cluster);
    }

    public void recordSession(int durationMinutes, boolean boss, boolean lowHealthMoment, boolean survivedLowHealth, boolean chain) {
        sessions++;
        bump(sessionLengths, durationMinutes < 20 ? "short" : durationMinutes < 45 ? "medium" : "long");
        if (boss) bossEncounters++;
        if (lowHealthMoment) {
            lowHealthMoments++;
            if (survivedLowHealth) {
                lowHealthSurvivals++;
            }
        }
        if (chain) chainMoments++;
    }

    public void closeSeasonSnapshot() {
        diversityTimeline.add(shannon(abilityFamilies) + shannon(branchPaths));
        dominantFamilyTimeline.add(dominantRate(abilityFamilies));
    }

    public Map<String, Object> asData() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("artifact", Map.of(
                "seed_distribution", seeds,
                "archetype_distribution", archetypes,
                "evolution_stage_distribution", stages,
                "awakening_rates", awakenings,
                "fusion_rates", fusions,
                "drift_alignment_distribution", driftAlignments,
                "branch_path_distribution", branchPaths,
                "mutation_counts", mutationCounts,
                "memory_profile_summaries", memoryProfiles
        ));
        root.put("ability", Map.of(
                "family_distribution", abilityFamilies,
                "trigger_distribution", triggers,
                "mechanic_distribution", mechanics,
                "branch_diversity", shannon(branchPaths),
                "memory_driven_ability_frequency", triggers.getOrDefault("on_memory_event", 0)
        ));
        root.put("player", Map.of(
                "playstyle_cluster_distribution", playstyleClusters,
                "session_length_distribution", sessionLengths,
                "boss_engagement_rate", rate(bossEncounters, sessions),
                "low_health_survival_rate", rate(lowHealthSurvivals, Math.max(1, lowHealthMoments)),
                "kill_chain_rate", rate(chainMoments, sessions)
        ));
        root.put("lineage", Map.of(
                "lineage_count", lineageCounts.size(),
                "lineage_distribution", lineageCounts,
                "lineage_depth_distribution", lineageDepth,
                "lineage_extinction_rate", rate(lineageExtinctions, Math.max(1, lineageCounts.size()))
        ));
        root.put("world", Map.of(
                "diversity_index_over_time", diversityTimeline,
                "branch_convergence_rate", 1.0D - Math.min(1.0D, shannon(branchPaths) / 4.0D),
                "dominant_family_rate", dominantRate(abilityFamilies),
                "dead_branch_rate", deadBranchRate(branchPaths),
                "late_season_meta_concentration", dominantFamilyTimeline.isEmpty() ? 0.0D : dominantFamilyTimeline.get(dominantFamilyTimeline.size() - 1),
                "long_run_fusion_adoption", rate(sumValue(fusions) - fusions.getOrDefault("none", 0), sumValue(fusions)),
                "long_run_awakening_adoption", rate(sumValue(awakenings) - awakenings.getOrDefault("dormant", 0), sumValue(awakenings))
        ));
        return root;
    }

    public Map<String, Integer> families() { return abilityFamilies; }
    public Map<String, Integer> branches() { return branchPaths; }
    public Map<String, Integer> mutations() { return mutationCounts; }
    public Map<String, Integer> triggers() { return triggers; }
    public Map<String, Integer> mechanics() { return mechanics; }
    public Map<String, Integer> memories() { return memoryProfiles; }
    public List<Double> diversityTimeline() { return diversityTimeline; }
    public List<Double> dominantFamilyTimeline() { return dominantFamilyTimeline; }
    public Map<String, Integer> lineageCounts() { return lineageCounts; }

    private int sumValue(Map<String, Integer> map) {
        return map.values().stream().mapToInt(Integer::intValue).sum();
    }

    private double deadBranchRate(Map<String, Integer> distribution) {
        int total = sumValue(distribution);
        if (total == 0 || distribution.isEmpty()) {
            return 0.0D;
        }
        long dead = distribution.values().stream().filter(v -> v <= 1).count();
        return dead / (double) distribution.size();
    }

    private double dominantRate(Map<String, Integer> distribution) {
        int total = sumValue(distribution);
        if (total == 0 || distribution.isEmpty()) {
            return 0.0D;
        }
        int dominant = distribution.values().stream().max(Integer::compareTo).orElse(0);
        return dominant / (double) total;
    }

    private double shannon(Map<String, Integer> distribution) {
        int total = sumValue(distribution);
        if (total == 0) return 0.0D;
        double out = 0.0D;
        for (int value : distribution.values()) {
            double p = value / (double) total;
            out -= p * Math.log(p);
        }
        return out;
    }

    private double rate(int numerator, int denominator) {
        if (denominator <= 0) return 0.0D;
        return numerator / (double) denominator;
    }

    private void bump(Map<String, Integer> map, String key) {
        map.merge(key, 1, Integer::sum);
    }
}
