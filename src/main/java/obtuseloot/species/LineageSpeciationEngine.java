package obtuseloot.species;

import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;

import java.util.LinkedHashMap;
import java.util.Map;

public class LineageSpeciationEngine {
    private static final double COMPATIBILITY_THRESHOLD = 0.64D;
    private static final double STRICT_COMPATIBILITY_THRESHOLD = 0.72D;
    private static final int PERSISTENCE_GENERATIONS = 4;
    private static final int MIN_OBSERVATIONS = 6;
    private static final double MIN_COMPONENT_DIVERGENCE = 0.08D;
    private static final double MIN_NICHE_DIVERGENCE = 0.12D;
    private static final double MIN_ENV_DIVERGENCE = 0.09D;

    private final SpeciesCompatibilityModel compatibilityModel = new SpeciesCompatibilityModel();
    private final Map<String, DivergenceState> divergenceStates = new LinkedHashMap<>();

    public ArtifactSpecies evaluate(Artifact artifact,
                                    ArtifactLineage lineage,
                                    SpeciesRegistry speciesRegistry,
                                    ArtifactPopulationSignature currentSignature,
                                    ArtifactPopulationSignature speciesSignature) {
        ArtifactSpecies currentSpecies = speciesRegistry.resolveSpecies(artifact, lineage);
        SpeciesCompatibilityModel.DivergenceProfile profile = compatibilityModel.divergenceProfile(currentSignature, speciesSignature);
        double compatibility = profile.weightedDistance();
        artifact.setLastSpeciesCompatibilityDistance(compatibility);

        String key = lineage.lineageId() + "::" + currentSpecies.speciesId();
        DivergenceState state = divergenceStates.computeIfAbsent(key, ignored -> new DivergenceState());
        state.observations++;

        double nicheOverlap = profileOverlap(currentSignature.branchPreferences(), speciesSignature.branchPreferences());
        boolean sameNiche = nicheOverlap >= 0.72D;
        double threshold = sameNiche ? STRICT_COMPATIBILITY_THRESHOLD : COMPATIBILITY_THRESHOLD;
        if (compatibility >= threshold) {
            state.persistenceStreak++;
        } else {
            state.persistenceStreak = Math.max(0, state.persistenceStreak - 1);
        }

        boolean persistent = state.persistenceStreak >= PERSISTENCE_GENERATIONS;
        boolean stableNiche = state.observations >= MIN_OBSERVATIONS;
        boolean notSpike = state.persistenceStreak * 2 >= state.observations;
        boolean meaningfulDivergence = hasMeaningfulDivergence(profile);
        boolean ecologicalDivergence = profile.nicheOccupancy() >= MIN_NICHE_DIVERGENCE
                && profile.environment() >= MIN_ENV_DIVERGENCE;

        if (!(persistent && stableNiche && notSpike && meaningfulDivergence && ecologicalDivergence)) {
            return currentSpecies;
        }

        Map<String, Double> divergenceSnapshot = new LinkedHashMap<>();
        divergenceSnapshot.put("compatibility", compatibility);
        divergenceSnapshot.put("genome", profile.genome());
        divergenceSnapshot.put("gates", profile.gates());
        divergenceSnapshot.put("triggers", profile.triggers());
        divergenceSnapshot.put("mechanics", profile.mechanics());
        divergenceSnapshot.put("branches", profile.branches());
        divergenceSnapshot.put("environment", profile.environment());
        divergenceSnapshot.put("nicheOccupancy", profile.nicheOccupancy());
        divergenceSnapshot.put("sameNichePressure", sameNiche ? 1.0D : 0.0D);
        divergenceSnapshot.put("compatibilityThreshold", threshold);
        divergenceSnapshot.put("nicheOverlap", nicheOverlap);
        divergenceSnapshot.put("persistence", (double) state.persistenceStreak);
        divergenceSnapshot.put("observations", (double) state.observations);

        Map<String, Double> tendencyProfile = new LinkedHashMap<>(deriveTendencies(currentSignature));
        tendencyProfile.put("nicheMigrationPressure", Math.max(0.0D, profile.nicheOccupancy() - nicheOverlap));
        tendencyProfile.put("environmentShift", profile.environment());
        state.persistenceStreak = 0;
        state.observations = 0;
        return speciesRegistry.registerSplit(artifact, lineage, currentSpecies, divergenceSnapshot, tendencyProfile);
    }

    private boolean hasMeaningfulDivergence(SpeciesCompatibilityModel.DivergenceProfile profile) {
        int components = 0;
        if (profile.genome() >= MIN_COMPONENT_DIVERGENCE) components++;
        if (profile.gates() >= MIN_COMPONENT_DIVERGENCE) components++;
        if (profile.triggers() >= MIN_COMPONENT_DIVERGENCE) components++;
        if (profile.mechanics() >= MIN_COMPONENT_DIVERGENCE) components++;
        if (profile.branches() >= MIN_COMPONENT_DIVERGENCE) components++;
        if (profile.environment() >= MIN_COMPONENT_DIVERGENCE) components++;
        return components >= 3;
    }

    private double profileOverlap(Map<String, Double> left, Map<String, Double> right) {
        if (left.isEmpty() && right.isEmpty()) {
            return 0.0D;
        }
        double overlap = 0.0D;
        for (String key : left.keySet()) {
            if (!right.containsKey(key)) {
                continue;
            }
            overlap += Math.min(left.getOrDefault(key, 0.0D), right.getOrDefault(key, 0.0D));
        }
        return Math.max(0.0D, Math.min(1.0D, overlap));
    }

    private Map<String, Double> deriveTendencies(ArtifactPopulationSignature signature) {
        return Map.of(
                "gateAffinity", topWeight(signature.gateProfile()),
                "branchBias", topWeight(signature.branchPreferences()),
                "environmentBias", topWeight(signature.environmentalProfile()));
    }

    private double topWeight(Map<String, Double> weights) {
        return weights.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.5D);
    }

    private static final class DivergenceState {
        private int persistenceStreak;
        private int observations;
    }
}
