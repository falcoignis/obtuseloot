package obtuseloot.names;

public record ArtifactPersonalityProfile(
        ArtifactPersonalityTrait dominant,
        ArtifactPersonalityTrait secondary,
        double intensity
) {
    public ArtifactPersonalityProfile {
        intensity = Math.max(0.0D, Math.min(1.0D, intensity));
    }

    public boolean isAny(ArtifactPersonalityTrait trait) {
        return dominant == trait || secondary == trait;
    }
}
