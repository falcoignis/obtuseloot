package com.falcoignis.obtuseloot.lore;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
import com.falcoignis.obtuseloot.drift.DriftEngine;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class LoreEngine {
    private LoreEngine() {
    }

    public static void refreshLore(Player player, ArtifactReputation rep) {
        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());
        List<String> lore = buildLore(artifact, rep);
        if (!lore.isEmpty()) {
            player.sendActionBar(net.kyori.adventure.text.Component.text(lore.get(0)));
        }
    }

    public static List<String> buildLore(Artifact artifact, ArtifactReputation rep) {
        List<String> lines = new ArrayList<>();
        lines.add("The blade remembers every battle fought in desperation.");
        lines.add("Seed " + artifact.getSeed() + ", Evolution " + artifact.getEvolutionPath() + ", Awakening " + artifact.getAwakeningPath() + ".");
        lines.add(String.format("Drift chance %.2f%%, precision %.3f, chaos %.3f.", DriftEngine.driftChance(rep) * 100.0, rep.precision(), rep.chaos()));
        return lines;
    }
}
