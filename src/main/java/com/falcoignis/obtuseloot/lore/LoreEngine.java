package com.falcoignis.obtuseloot.lore;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class LoreEngine {
    private LoreEngine() {}

    public static void refreshLore(Player player, ArtifactReputation reputation) {
        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());
        List<String> lore = buildLore(artifact, reputation);
        // Placeholder hook: this is where ItemMeta lore would be updated.
        if (!lore.isEmpty()) {
            player.setCustomNameVisible(player.isCustomNameVisible());
        }
    }

    public static List<String> buildLore(Artifact artifact, ArtifactReputation reputation) {
        List<String> lines = new ArrayList<>();
        lines.add("Stage: " + artifact.getEvolutionStage());
        lines.add("Reputation: " + reputation.score());
        lines.add("Awakened: " + artifact.isAwakened());
        return lines;
    }
}
