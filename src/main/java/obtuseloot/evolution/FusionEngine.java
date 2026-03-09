package obtuseloot.evolution;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;

public final class FusionEngine {
    private FusionEngine() {
    }

    public static void checkFusion(Player player, Artifact artifact, ArtifactReputation rep) {
        if (!"none".equals(artifact.getFusionPath())) return;
        if (rep.getTotalScore() > 120 && !"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            artifact.setFusionPath(artifact.getArchetypePath() + "-convergence");
            artifact.setEvolutionPath("fused-" + artifact.getArchetypePath());
        }
    }
}
