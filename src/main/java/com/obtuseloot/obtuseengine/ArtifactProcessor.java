package com.obtuseloot.obtuseengine;

import com.obtuseloot.awakening.AwakeningEngine;
import com.obtuseloot.config.RuntimeSettings;
import com.obtuseloot.drift.DriftEngine;
import com.obtuseloot.evolution.EvolutionEngine;
import com.obtuseloot.evolution.FusionEngine;
import com.obtuseloot.lore.LoreEngine;
import com.obtuseloot.reputation.ArtifactReputation;
import com.obtuseloot.reputation.ReputationManager;

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
