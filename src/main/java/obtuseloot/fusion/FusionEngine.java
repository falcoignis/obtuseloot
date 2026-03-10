package obtuseloot.fusion;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

import java.util.List;

public class FusionEngine {
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();

    public boolean evaluate(Player player, Artifact artifact, ArtifactReputation rep) {
        return evaluateInternal(player, artifact, rep);
    }

    public boolean evaluateSimulation(Artifact artifact, ArtifactReputation rep) {
        return evaluateInternal(null, artifact, rep);
    }

    private boolean evaluateInternal(Player player, Artifact artifact, ArtifactReputation rep) {
        if (!ArtifactEligibility.isEvolutionEligible(artifact) || !"none".equalsIgnoreCase(artifact.getFusionPath())) {
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
            artifact.addLoreHistory(textResolver.compose(artifact, ArtifactTextChannel.FUSION, recipe.id()));
            artifact.addNotableEvent("fusion." + recipe.id());
            if (player != null) {
                player.sendMessage("§6" + textResolver.compose(artifact, ArtifactTextChannel.FUSION, recipe.id()));
            }
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
