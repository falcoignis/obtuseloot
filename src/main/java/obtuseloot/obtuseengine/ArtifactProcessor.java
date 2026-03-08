package obtuseloot.obtuseengine;

import obtuseloot.awakening.AwakeningEngine;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.drift.DriftEngine;
import obtuseloot.evolution.EvolutionEngine;
import obtuseloot.evolution.FusionEngine;
import obtuseloot.lore.LoreEngine;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.reputation.ReputationManager;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Hot-path orchestration for artifact progression.
 *
 * <p>This class intentionally avoids allocations and complex branching to remain server-tick friendly.
 */
public final class ArtifactProcessor {
    private ArtifactProcessor() {
    }

    public static void processKill(Player player) {
        ArtifactReputation rep = ReputationManager.get(player.getUniqueId());
        rep.recordKill();

        EvolutionEngine.checkEvolution(player, rep);

        if (DriftEngine.shouldDrift(rep)) {
            DriftEngine.applyDrift(player);
        }

        AwakeningEngine.checkAwakening(player, rep);
        FusionEngine.checkFusion(player, rep);
        LoreEngine.refreshLore(player, rep);
    }

    public static void processCombat(Player player, EntityDamageByEntityEvent event) {
        ArtifactReputation rep = ReputationManager.get(player.getUniqueId());
        RuntimeSettings.Snapshot config = RuntimeSettings.get();

        if (event.getFinalDamage() >= config.precisionThresholdDamage()) {
            rep.recordPrecision();
        } else {
            rep.recordBrutality();
        }

        rep.recordConsistency();
        LoreEngine.refreshLore(player, rep);
    }
}
