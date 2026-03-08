package com.falcoignis.obtuseloot.artifacts;

import java.util.UUID;

public final class ArtifactGenerator {
    private ArtifactGenerator() {}

    public static Artifact generateFor(UUID ownerId) {
        return new Artifact(ownerId);
    }
}
