package com.falcoignis.obtuseloot.evolution;

import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

public final class HybridEvolutionResolver {
    private HybridEvolutionResolver() {}

    public static String resolve(ArtifactReputation reputation) {
        if (reputation.kills() > 100) {
            return "hybrid-warform";
        }
        return "hybrid-balanced";
    }
}
