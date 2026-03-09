package obtuseloot.fusion;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;

import java.util.List;

public class FusionEngine {

    public boolean evaluate(Player player, Artifact artifact, ArtifactReputation rep) {
        if (!ArtifactEligibility.isEvolutionEligible(artifact)) {
            return false;
        }
        if (!"none".equalsIgnoreCase(artifact.getFusionPath())) {
            return false;
        }

        for (FusionRecipe recipe : recipes()) {
            if (!matches(recipe, artifact, rep)) {
                continue;
            }

            artifact.setFusionPath(recipe.id());
            if (recipe.overridesEvolution()) {
                artifact.setEvolutionPath("fused-" + recipe.id());
            }
            artifact.addLoreHistory("Fusion: " + recipe.id());
            artifact.addNotableEvent("fusion." + recipe.id());
            player.sendMessage("§6Fusion attained: " + recipe.id());
            return true;
        }

        return false;
    }

    private boolean matches(FusionRecipe recipe, Artifact artifact, ArtifactReputation rep) {
        return recipe.requiredArchetype().equalsIgnoreCase(artifact.getArchetypePath())
                && rep.getTotalScore() >= recipe.minTotalScore()
                && rep.getBossKills() >= recipe.minBossKills()
                && !"dormant".equalsIgnoreCase(artifact.getAwakeningPath());
    }

    private List<FusionRecipe> recipes() {
        return List.of(
                new FusionRecipe("convergence", "vanguard", 90, 1, true),
                new FusionRecipe("bloodstorm", "ravager", 95, 1, true),
                new FusionRecipe("voidcrown", "harbinger", 105, 2, true)
        );
    }

    private record FusionRecipe(String id, String requiredArchetype, int minTotalScore, int minBossKills,
                                boolean overridesEvolution) {
    }
}
