package com.falcoignis.obtuseloot.lore;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
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
        lines.add("The artifact hums with a " + artifact.getEvolutionPath() + " resonance.");
        if (!"dormant".equals(artifact.getAwakeningPath())) {
            lines.add("It now answers the path of " + artifact.getAwakeningPath() + ".");
        }
        lines.add("Its legacy reflects precision " + rep.precision() + " and chaos " + rep.chaos() + ".");
        return lines;
    }
}
