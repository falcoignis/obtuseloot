package com.falcoignis.obtuseloot.debug;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;

import java.util.UUID;

public final class ArtifactDebugger {
    private ArtifactDebugger() {}

    public static String describe(UUID playerId) {
        Artifact artifact = ArtifactManager.getOrCreate(playerId);
        return "Artifact{" +
                "owner=" + artifact.getOwner() +
                ", stage='" + artifact.getEvolutionStage() + '\'' +
                ", awakened=" + artifact.isAwakened() +
                '}';
    }
}
