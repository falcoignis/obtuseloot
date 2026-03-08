package com.falcoignis.obtuseloot.obtuseengine;

import com.falcoignis.obtuseloot.awakening.AwakeningEngine;
import com.falcoignis.obtuseloot.drift.DriftEngine;
import com.falcoignis.obtuseloot.evolution.EvolutionEngine;
import com.falcoignis.obtuseloot.lore.LoreEngine;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;
import com.falcoignis.obtuseloot.reputation.ReputationManager;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ArtifactProcessor {

    public static void processKill(Player player) {
        ArtifactReputation rep = ReputationManager.get(player.getUniqueId());
        rep.recordKill();

        EvolutionEngine.checkEvolution(player, rep);

        if (DriftEngine.shouldDrift(rep)) {
            DriftEngine.applyDrift(player);
        }

        AwakeningEngine.checkAwakening(player, rep);
        LoreEngine.refreshLore(player, rep);
    }

    public static void processCombat(Player player, EntityDamageByEntityEvent event) {
        ArtifactReputation rep = ReputationManager.get(player.getUniqueId());
        if (event.getFinalDamage() >= 10.0D) {
            rep.recordPrecision();
        } else {
            rep.recordBrutality();
        }
        rep.recordConsistency();
        LoreEngine.refreshLore(player, rep);
    }
}
