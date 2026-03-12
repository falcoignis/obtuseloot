package obtuseloot.evolution;

import java.util.Map;
import java.util.Set;

public record ArtifactNicheProfile(
        MechanicNicheTag dominantNiche,
        Set<MechanicNicheTag> niches,
        Map<MechanicNicheTag, Double> nicheScores,
        NicheSpecializationProfile specialization
) {
}
