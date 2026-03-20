package obtuseloot.artifacts;

import java.util.Objects;

public record ArtifactIdentityTransition(
        Artifact discarded,
        Artifact replacement,
        String reason
) {
    public ArtifactIdentityTransition {
        Objects.requireNonNull(replacement, "replacement");
        reason = (reason == null || reason.isBlank()) ? "artifact-identity-transition" : reason;
        if (discarded != null && discarded == replacement) {
            throw new IllegalArgumentException("Artifact identity transition must replace the discarded instance.");
        }
    }

    public static ArtifactIdentityTransition recreate(Artifact discarded, Artifact replacement) {
        return new ArtifactIdentityTransition(discarded, replacement, "artifact-recreate");
    }

    public static ArtifactIdentityTransition regenerate(Artifact discarded, Artifact replacement) {
        return new ArtifactIdentityTransition(discarded, replacement, "artifact-regenerate");
    }

    public boolean preservesOwnerStorageContinuity() {
        if (discarded == null) {
            return false;
        }
        return Objects.equals(discarded.getOwnerId(), replacement.getOwnerId())
                && Objects.equals(discarded.getArtifactStorageKey(), replacement.getArtifactStorageKey());
    }
}
