package obtuseloot.analytics;

import java.util.List;

public record EcologyDiagnosticSnapshot(
        double endArtifacts,
        Double endSpecies,
        double latestTnt,
        double latestNser,
        double dominantNicheShare,
        double dominantSpeciesShare,
        double dominantAttractorShare,
        int nicheCount,
        int speciesCount,
        List<Double> nserTrend,
        boolean noveltyPersistenceWeak,
        int relabelingEvents,
        EcologyDiagnosticState state,
        double confidence,
        List<String> warningFlags,
        String explanation,
        String recommendedNextAction
) {}

