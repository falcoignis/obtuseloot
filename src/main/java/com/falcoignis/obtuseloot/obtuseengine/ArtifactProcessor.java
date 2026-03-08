package com.falcoignis.obtuseloot.obtuseengine;

import com.falcoignis.obtuseloot.awakening.AwakeningEngine;
import com.falcoignis.obtuseloot.drift.DriftEngine;
import com.falcoignis.obtuseloot.evolution.EvolutionEngine;
import com.falcoignis.obtuseloot.lore.LoreEngine;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;
import com.falcoignis.obtuseloot.reputation.ReputationManager;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class ArtifactProcessor {

    private ArtifactProcessor() {}

    public static void processKill(Player player) {
        ArtifactReputation rep = ReputationManager.get(player.getUniqueId());
        rep.recordKill();

        EvolutionEngine.checkEvolution(player, rep);

        if (DriftEngine.shouldDrift(rep)) {
            DriftEngine.applyDrift(player, rep);
        }

        AwakeningEngine.checkAwakening(player, rep);
        LoreEngine.refreshLore(player, rep);
    }

    public static void processCombatHit(Player player, EntityDamageByEntityEvent event) {
        ArtifactReputation rep = ReputationManager.get(player.getUniqueId());
        if (event.getFinalDamage() >= 8.0D) {
            rep.recordHeadshot();
        }
        LoreEngine.refreshLore(player, rep);
    }
}
