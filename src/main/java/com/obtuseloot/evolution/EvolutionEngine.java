package com.obtuseloot.evolution;

import com.obtuseloot.artifacts.Artifact;
import com.obtuseloot.artifacts.ArtifactManager;
import com.obtuseloot.config.RuntimeSettings;
import com.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

public final class EvolutionEngine {
    private EvolutionEngine() {
    }

    public static void checkEvolution(Player player, ArtifactReputation rep) {
        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());
        RuntimeSettings.Snapshot settings = RuntimeSettings.get();
        int score = rep.score();

        if (score < settings.archetypeScore()) {
            return;
        }

        String archetype = ArchetypeResolver.resolve(rep, settings.archetypeDominanceDelta());
        artifact.setArchetypePath(archetype);

        if (score >= settings.hybridScore()) {
            artifact.setEvolutionPath(HybridEvolutionResolver.resolve(archetype, rep));
        } else if (score >= settings.mythicScore()) {
            artifact.setEvolutionPath(archetype + "-ascendant");
        } else if (score >= settings.temperedScore()) {
            artifact.setEvolutionPath(archetype + "-adept");
        } else {
            artifact.setEvolutionPath(archetype + "-initiate");
        }
    }
}
