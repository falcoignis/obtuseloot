package com.falcoignis.obtuseloot.debug;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
import com.falcoignis.obtuseloot.drift.DriftEngine;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;
import com.falcoignis.obtuseloot.reputation.ReputationManager;

import java.util.UUID;

public final class ArtifactDebugger {
    private ArtifactDebugger() {
    }

    public static String describe(UUID playerId) {
        Artifact artifact = ArtifactManager.getOrCreate(playerId);
        ArtifactReputation rep = ReputationManager.get(playerId);
        return "Artifact: " + artifact.getSeed()
                + " | Evolution: " + artifact.getEvolutionPath()
                + " | Awakened: " + (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath()))
                + " | Precision: " + String.format("%.3f", rep.precision())
                + " | Brutality: " + String.format("%.3f", rep.brutality())
                + " | Survival: " + String.format("%.3f", rep.survival())
                + " | Mobility: " + String.format("%.3f", rep.mobility())
                + " | Chaos: " + String.format("%.3f", rep.chaos())
                + " | Risk: " + String.format("%.3f", rep.risk())
                + " | Mastery: " + String.format("%.3f", rep.mastery())
                + " | DriftChance: " + String.format("%.2f%%", DriftEngine.driftChance(rep) * 100.0)
                + " | AwakeningPath: " + artifact.getAwakeningPath();
    }
}
