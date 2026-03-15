package obtuseloot.fusion;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FusionEngine {
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();

    // Diagnostic counters for fusion pathway observability
    private final AtomicInteger fusionAttempted = new AtomicInteger();
    private final AtomicInteger fusionBlocked = new AtomicInteger();
    private final AtomicInteger fusionPrereqFailed = new AtomicInteger();
    private final AtomicInteger fusionPairFound = new AtomicInteger();
    private final AtomicInteger fusionApplied = new AtomicInteger();

    public boolean evaluate(Player player, Artifact artifact, ArtifactReputation rep) {
        return evaluateInternal(player, artifact, rep);
    }

    public boolean evaluateSimulation(Artifact artifact, ArtifactReputation rep) {
        return evaluateInternal(null, artifact, rep);
    }

    private boolean evaluateInternal(Player player, Artifact artifact, ArtifactReputation rep) {
        fusionAttempted.incrementAndGet();

        if (!ArtifactEligibility.isEvolutionEligible(artifact) || !"none".equalsIgnoreCase(artifact.getFusionPath())) {
            fusionBlocked.incrementAndGet();
            return false;
        }

        for (FusionRecipe recipe : recipes()) {
            if (!matches(recipe, artifact, rep)) {
                continue;
            }

            fusionPairFound.incrementAndGet();
            artifact.setFusionPath(recipe.id());
            if (recipe.overridesEvolution()) {
                artifact.setEvolutionPath("fused-" + recipe.id());
            }
            artifact.addLoreHistory(textResolver.compose(artifact, ArtifactTextChannel.FUSION, recipe.id()));
            artifact.addNotableEvent("fusion." + recipe.id());
            if (player != null) {
                player.sendMessage("§6" + textResolver.compose(artifact, ArtifactTextChannel.FUSION, recipe.id()));
            }
            fusionApplied.incrementAndGet();
            return true;
        }

        fusionPrereqFailed.incrementAndGet();
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
                new FusionRecipe("voidcrown", "harbinger", 105, 2, true),
                // Directed-archetype recipes: deadeye and strider are the natural
                // archetypes for ritualist/gatherer and explorer scenarios respectively.
                // Without these, directed scenarios can never reach fusion because
                // their behavior profiles never produce vanguard/ravager/harbinger.
                new FusionRecipe("eclipseshot", "deadeye", 88, 1, true),
                new FusionRecipe("stormrider", "strider", 88, 1, true)
        );
    }

    /** Returns a snapshot of fusion diagnostic counters for telemetry/reporting. */
    public Map<String, Integer> diagnosticCounters() {
        return Map.of(
                "fusion_attempted", fusionAttempted.get(),
                "fusion_blocked", fusionBlocked.get(),
                "fusion_prereq_failed", fusionPrereqFailed.get(),
                "fusion_pair_found", fusionPairFound.get(),
                "fusion_applied", fusionApplied.get()
        );
    }

    private record FusionRecipe(String id, String requiredArchetype, int minTotalScore, int minBossKills,
                                boolean overridesEvolution) {
    }
}
