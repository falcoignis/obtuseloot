package obtuseloot.drift;

import obtuseloot.config.RuntimeSettings;
import obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public final class DriftEngine {
    private DriftEngine() {
    }

    public static boolean shouldDrift(ArtifactReputation rep) {
        RuntimeSettings.Snapshot settings = RuntimeSettings.get();
        double driftChance = settings.driftBaseChance()
                + (rep.chaos() * settings.driftChaosMultiplier())
                - (rep.consistency() * settings.driftConsistencyReduction());
        double clamped = Math.max(0.0, Math.min(settings.driftMaxChance(), driftChance));
        return ThreadLocalRandom.current().nextDouble() < clamped;
    }

    public static void applyDrift(Player player) {
        player.sendMessage("§5Your artifact drifts into a new unstable form...");
    }
}
