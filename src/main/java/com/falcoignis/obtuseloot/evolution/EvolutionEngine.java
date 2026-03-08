package com.falcoignis.obtuseloot.evolution;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

public final class EvolutionEngine {
    private EvolutionEngine() {}

    public static void checkEvolution(Player player, ArtifactReputation reputation) {
        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());
        if (reputation.score() >= 250) {
            artifact.setEvolutionStage(HybridEvolutionResolver.resolve(reputation));
        } else if (reputation.score() >= 100) {
            artifact.setEvolutionStage("ascended");
        } else if (reputation.score() >= 25) {
            artifact.setEvolutionStage("refined");
        }
    }
}
