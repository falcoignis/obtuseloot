package obtuseloot.convergence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactArchetypeValidator;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ConvergenceEngine {
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();

    // Diagnostic counters for convergence pathway observability
    private final AtomicInteger convergenceAttempted = new AtomicInteger();
    private final AtomicInteger convergenceBlocked = new AtomicInteger();
    private final AtomicInteger convergencePrereqFailed = new AtomicInteger();
    private final AtomicInteger convergencePairFound = new AtomicInteger();
    private final AtomicInteger convergenceApplied = new AtomicInteger();

    public boolean evaluate(Player player, Artifact artifact, ArtifactReputation rep) {
        return evaluateInternal(player, artifact, rep);
    }

    public boolean evaluateSimulation(Artifact artifact, ArtifactReputation rep) {
        return evaluateInternal(null, artifact, rep);
    }

    private boolean evaluateInternal(Player player, Artifact artifact, ArtifactReputation rep) {
        convergenceAttempted.incrementAndGet();

        ArtifactArchetypeValidator.requireValid(artifact, "convergence evaluation");
        if (!"none".equalsIgnoreCase(artifact.getConvergencePath())) {
            convergenceBlocked.incrementAndGet();
            return false;
        }

        for (ConvergenceRecipe recipe : recipes()) {
            if (!matches(recipe, artifact, rep)) {
                continue;
            }

            convergencePairFound.incrementAndGet();
            artifact.setConvergencePath(recipe.id());
            if (recipe.overridesEvolution()) {
                artifact.setEvolutionPath("fused-" + recipe.id());
            }
            artifact.addLoreHistory(textResolver.compose(artifact, ArtifactTextChannel.CONVERGENCE, recipe.id()));
            artifact.addNotableEvent("convergence." + recipe.id());
            if (player != null) {
                player.sendMessage("§6" + textResolver.compose(artifact, ArtifactTextChannel.CONVERGENCE, recipe.id()));
            }
            convergenceApplied.incrementAndGet();
            return true;
        }

        convergencePrereqFailed.incrementAndGet();
        return false;
    }

    private boolean matches(ConvergenceRecipe recipe, Artifact artifact, ArtifactReputation rep) {
        return recipe.requiredArchetype().equalsIgnoreCase(artifact.getArchetypePath())
                && rep.getTotalScore() >= recipe.minTotalScore()
                && rep.getBossKills() >= recipe.minBossKills()
                && !"dormant".equalsIgnoreCase(artifact.getAwakeningPath());
    }

    private List<ConvergenceRecipe> recipes() {
        return List.of(
                new ConvergenceRecipe("convergence", "vanguard", 90, 1, true),
                new ConvergenceRecipe("bloodstorm", "ravager", 95, 1, true),
                new ConvergenceRecipe("voidcrown", "harbinger", 105, 2, true),
                // Directed-archetype recipes: deadeye and strider are the natural
                // archetypes for ritualist/gatherer and explorer scenarios respectively.
                // Without these, directed scenarios can never reach convergence because
                // their behavior profiles never produce vanguard/ravager/harbinger.
                new ConvergenceRecipe("eclipseshot", "deadeye", 88, 1, true),
                new ConvergenceRecipe("stormrider", "strider", 88, 1, true)
        );
    }

    /** Returns a snapshot of convergence diagnostic counters for telemetry/reporting. */
    public Map<String, Integer> diagnosticCounters() {
        return Map.of(
                "convergence_attempted", convergenceAttempted.get(),
                "convergence_blocked", convergenceBlocked.get(),
                "convergence_prereq_failed", convergencePrereqFailed.get(),
                "convergence_pair_found", convergencePairFound.get(),
                "convergence_applied", convergenceApplied.get()
        );
    }

    private record ConvergenceRecipe(String id, String requiredArchetype, int minTotalScore, int minBossKills,
                                boolean overridesEvolution) {
    }
}
