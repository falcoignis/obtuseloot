package com.falcoignis.obtuseloot.obtuseengine;

import com.falcoignis.obtuseloot.awakening.AwakeningEngine;
import com.falcoignis.obtuseloot.config.RuntimeSettings;
import com.falcoignis.obtuseloot.drift.DriftEngine;
import com.falcoignis.obtuseloot.evolution.EvolutionEngine;
import com.falcoignis.obtuseloot.evolution.FusionEngine;
import com.falcoignis.obtuseloot.lore.LoreEngine;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;
import com.falcoignis.obtuseloot.reputation.ReputationManager;

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
