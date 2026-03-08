package com.falcoignis.obtuseloot.evolution;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class EvolutionEngine {
    private EvolutionEngine() {
    }

    public static boolean checkEvolution(Player player, ArtifactReputation rep) {
        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());
        String before = artifact.getEvolutionPath();

        String primary = choosePrimaryEvolution(rep);
        String evolved = HybridEvolutionResolver.resolve(primary, artifact.getEvolutionPath());
        artifact.setEvolutionPath(evolved);

        return !before.equals(evolved);
    }

    private static String choosePrimaryEvolution(ArtifactReputation rep) {
        double baseWeight = 1.0;
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("Marksman", baseWeight + rep.precision() * 2.0);
        weights.put("Berserker", baseWeight + rep.brutality() * 2.0);
        weights.put("Survivor", baseWeight + rep.survival() * 2.0);
        weights.put("Acrobat", baseWeight + rep.mobility() * 2.0);

        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double roll = ThreadLocalRandom.current().nextDouble() * total;
        double cursor = 0.0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            cursor += entry.getValue();
            if (roll <= cursor) {
                return entry.getKey();
            }
        }
        return "Marksman";
    }
}
