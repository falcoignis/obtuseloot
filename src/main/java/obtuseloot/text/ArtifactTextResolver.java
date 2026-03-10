package obtuseloot.text;

import obtuseloot.artifacts.Artifact;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.names.ArtifactNaming;

public class ArtifactTextResolver {
    private final ArtifactTextIdentityResolver identityResolver = new ArtifactTextIdentityResolver();
    private final ArtifactTextComposer composer = new ArtifactTextComposer();

    public ArtifactTextIdentity identityFor(Artifact artifact) {
        ArtifactNaming naming = artifact.getNaming();
        if (naming == null) {
            naming = ArtifactNameResolver.initialize(artifact);
            artifact.setNaming(naming);
        }
        return identityResolver.resolve(artifact, naming);
    }

    public String compose(Artifact artifact, ArtifactTextChannel channel, String context) {
        return composer.compose(artifact, identityFor(artifact), channel, context);
    }
}
