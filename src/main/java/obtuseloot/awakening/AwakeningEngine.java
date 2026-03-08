package obtuseloot.awakening;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

public final class AwakeningEngine {
    private AwakeningEngine() {
    }

    public static void checkAwakening(Player player, ArtifactReputation rep) {
        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());
        if (!"dormant".equals(artifact.getAwakeningPath())) {
            return;
        }

        RuntimeSettings.Snapshot settings = RuntimeSettings.get();

        if (rep.score() >= settings.executionersOathMinScore() && rep.bossKills() >= settings.executionersOathMinBossKills()) {
            artifact.setAwakeningPath("Executioner's Oath");
        } else if (rep.precision() >= settings.stormbladeMinPrecision() && rep.mobility() >= settings.stormbladeMinMobility()) {
            artifact.setAwakeningPath("Stormblade");
        } else if (rep.survival() >= settings.lastSurvivorMinSurvival()
                && rep.consistency() >= settings.lastSurvivorMinConsistency()) {
            artifact.setAwakeningPath("Last Survivor");
        }
    }
}
