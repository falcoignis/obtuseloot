package obtuseloot.evolution;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.config.RuntimeSettings.FusionRecipe;
import obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

public final class FusionEngine {
    private FusionEngine() {
    }

    public static void checkFusion(Player player, ArtifactReputation rep) {
        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());
        if (!"none".equals(artifact.getFusionPath())) {
            return;
        }

        RuntimeSettings.Snapshot settings = RuntimeSettings.get();
        if (rep.score() < settings.fusionMinScore()) {
            return;
        }

        for (FusionRecipe recipe : settings.fusionRecipes()) {
            if (matches(recipe, artifact, rep)) {
                artifact.setFusionPath(recipe.id());
                artifact.setEvolutionPath("fused-" + recipe.id());
                return;
            }
        }
    }

    private static boolean matches(FusionRecipe recipe, Artifact artifact, ArtifactReputation rep) {
        if (!recipe.archetype().equalsIgnoreCase(artifact.getArchetypePath())) {
            return false;
        }

        return rep.precision() >= recipe.minPrecision()
                && rep.brutality() >= recipe.minBrutality()
                && rep.survival() >= recipe.minSurvival()
                && rep.mobility() >= recipe.minMobility()
                && rep.chaos() >= recipe.minChaos()
                && rep.consistency() >= recipe.minConsistency()
                && rep.bossKills() >= recipe.minBossKills();
    }
}
