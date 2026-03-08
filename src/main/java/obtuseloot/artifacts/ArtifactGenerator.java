package obtuseloot.artifacts;

import obtuseloot.names.ArtifactNameGenerator;

import java.util.UUID;

public final class ArtifactGenerator {
    private ArtifactGenerator() {
    }

    public static Artifact generateFor(UUID playerId) {
        return new Artifact(playerId, ArtifactNameGenerator.generate(playerId));
    }
}
