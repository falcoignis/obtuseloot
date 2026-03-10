package obtuseloot.names;

public record ArtifactGeneratedIdentity(
        String displayName,
        String trueName,
        String epithet,
        String discoveryLine,
        String awakeningRevealLine,
        double implicationScore,
        ArtifactPersonalityProfile personality,
        ArtifactVoiceProfile voice
) {
}
