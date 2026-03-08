package com.falcoignis.obtuseloot.evolution;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

public final class EvolutionEngine {
    private EvolutionEngine() {
    }

    public static void checkEvolution(Player player, ArtifactReputation rep) {
        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());

        if (rep.score() >= 250) {
            artifact.setEvolutionPath(HybridEvolutionResolver.resolve(rep));
        } else if (rep.score() >= 120) {
            artifact.setEvolutionPath("mythic");
        } else if (rep.score() >= 40) {
            artifact.setEvolutionPath("tempered");
        }
    }
}
