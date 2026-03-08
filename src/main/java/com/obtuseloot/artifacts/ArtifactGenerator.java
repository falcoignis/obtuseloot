package com.obtuseloot.artifacts;

import com.obtuseloot.names.ArtifactNameGenerator;

import java.util.UUID;

public final class ArtifactGenerator {
    private ArtifactGenerator() {
    }

    public static Artifact generateFor(UUID playerId) {
        return new Artifact(playerId, ArtifactNameGenerator.generate(playerId));
    }
}
