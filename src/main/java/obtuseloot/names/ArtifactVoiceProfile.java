package obtuseloot.names;

public record ArtifactVoiceProfile(
        ArtifactVoiceRegister primary,
        ArtifactVoiceRegister secondary,
        boolean titleHeavy,
        double softness
) {
    public ArtifactVoiceProfile {
        softness = Math.max(0.0D, Math.min(1.0D, softness));
    }
}
