package com.falcoignis.obtuseloot.evolution;

import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

public final class HybridEvolutionResolver {
    private HybridEvolutionResolver() {
    }

    public static String resolve(ArtifactReputation rep) {
        if (rep.precision() > rep.brutality() && rep.survival() > rep.mobility()) {
            return "warden-scholar";
        }
        if (rep.brutality() > rep.precision() && rep.chaos() > rep.consistency()) {
            return "berserker-tempest";
        }
        return "balanced-hybrid";
    }
}
