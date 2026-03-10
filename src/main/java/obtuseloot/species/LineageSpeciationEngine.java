package obtuseloot.species;

import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;

import java.util.LinkedHashMap;
import java.util.Map;

public class LineageSpeciationEngine {
    private static final double COMPATIBILITY_THRESHOLD = 0.66D;
    private static final double STRICT_COMPATIBILITY_THRESHOLD = 0.74D;
    private static final int PERSISTENCE_GENERATIONS = 5;
    private static final int MIN_OBSERVATIONS = 8;
    private static final double MIN_COMPONENT_DIVERGENCE = 0.09D;
    private static final double MIN_NICHE_DIVERGENCE = 0.14D;
    private static final double MIN_ENV_DIVERGENCE = 0.10D;
    private static final double STABLE_NICHE_TENDENCY = 0.62D;

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

        double branchOverlap = profileOverlap(currentSignature.branchPreferences(), speciesSignature.branchPreferences());
        double environmentOverlap = profileOverlap(currentSignature.environmentalProfile(), speciesSignature.environmentalProfile());
        double nicheOverlap = (branchOverlap * 0.65D) + (environmentOverlap * 0.35D);
        boolean sameNiche = nicheOverlap >= 0.72D;
        double threshold = sameNiche ? STRICT_COMPATIBILITY_THRESHOLD : COMPATIBILITY_THRESHOLD;
        if (compatibility >= threshold) {
            state.persistenceStreak++;
        } else {
            state.persistenceStreak = Math.max(0, state.persistenceStreak - 1);
        }

        if (profile.nicheOccupancy() >= MIN_NICHE_DIVERGENCE) {
            state.nicheStableObservations++;
        }
        boolean persistent = state.persistenceStreak >= PERSISTENCE_GENERATIONS;
        boolean stableNiche = state.observations >= MIN_OBSERVATIONS && (state.nicheStableObservations / (double) Math.max(1, state.observations)) >= STABLE_NICHE_TENDENCY;
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
        divergenceSnapshot.put("branchOverlap", branchOverlap);
        divergenceSnapshot.put("environmentOverlap", environmentOverlap);
        divergenceSnapshot.put("persistence", (double) state.persistenceStreak);
        divergenceSnapshot.put("observations", (double) state.observations);
        divergenceSnapshot.put("stableNicheRatio", state.nicheStableObservations / (double) Math.max(1, state.observations));

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
        return components >= 3 && profile.weightedDistance() >= COMPATIBILITY_THRESHOLD;
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
        private int nicheStableObservations;
    }
}
