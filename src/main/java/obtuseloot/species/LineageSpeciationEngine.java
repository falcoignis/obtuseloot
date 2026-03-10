package obtuseloot.species;

import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LineageSpeciationEngine {
    private static final double COMPATIBILITY_THRESHOLD = 0.67D;
    private static final int PERSISTENCE_GENERATIONS = 4;
    private static final int MIN_OBSERVATIONS = 6;

    private final SpeciesCompatibilityModel compatibilityModel = new SpeciesCompatibilityModel();
    private final Map<String, DivergenceState> divergenceStates = new LinkedHashMap<>();

    public ArtifactSpecies evaluate(Artifact artifact,
                                    ArtifactLineage lineage,
                                    SpeciesRegistry speciesRegistry,
                                    ArtifactPopulationSignature currentSignature,
                                    ArtifactPopulationSignature speciesSignature) {
        ArtifactSpecies currentSpecies = speciesRegistry.resolveSpecies(artifact, lineage);
        double compatibility = compatibilityModel.compatibilityDistance(currentSignature, speciesSignature);
        artifact.setLastSpeciesCompatibilityDistance(compatibility);

        String key = lineage.lineageId() + "::" + currentSpecies.speciesId();
        DivergenceState state = divergenceStates.computeIfAbsent(key, ignored -> new DivergenceState());
        state.observations++;
        if (compatibility >= COMPATIBILITY_THRESHOLD) {
            state.persistenceStreak++;
        } else {
            state.persistenceStreak = Math.max(0, state.persistenceStreak - 1);
        }

        boolean persistent = state.persistenceStreak >= PERSISTENCE_GENERATIONS;
        boolean stableNiche = state.observations >= MIN_OBSERVATIONS;
        boolean notSpike = state.persistenceStreak * 2 >= state.observations;

        if (!(persistent && stableNiche && notSpike)) {
            return currentSpecies;
        }

        Map<String, Double> divergenceSnapshot = Map.of(
                "compatibility", compatibility,
                "persistence", (double) state.persistenceStreak,
                "observations", (double) state.observations);
        Map<String, Double> tendencyProfile = deriveTendencies(currentSignature);
        state.persistenceStreak = 0;
        state.observations = 0;
        return speciesRegistry.registerSplit(artifact, lineage, currentSpecies, divergenceSnapshot, tendencyProfile);
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
