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
        if (discarded != null && Objects.equals(discarded.getArtifactStorageKey(), replacement.getArtifactStorageKey())) {
            var genome = discarded.getGenome();
            replacement.setGenome(new obtuseloot.abilities.genome.ArtifactGenome(replacement.getArtifactSeed(),
                    genome.traits(), genome.latentTraits(), genome.activatedLatentTraits()));
            replacement.restoreGenomeEvolution(discarded.getGenomeRevision(), discarded.getGenomeEvolutionState());
            replacement.setItemReputation(discarded.getItemReputation());
            replacement.setParentStorageKey(discarded.getParentStorageKey());
            replacement.setInheritanceGeneration(discarded.getInheritanceGeneration());
        }
    }

    public static ArtifactIdentityTransition recreate(Artifact discarded, Artifact replacement) {
        return baseline(discarded, replacement, "artifact-recreate");
    }

    public static ArtifactIdentityTransition regenerate(Artifact discarded, Artifact replacement) {
        return baseline(discarded, replacement, "artifact-regenerate");
    }

    public static ArtifactIdentityTransition baseline(Artifact discarded, Artifact replacement, String reason) {
        var transition = new ArtifactIdentityTransition(discarded, replacement, reason);
        replacement.restoreGenomeEvolution(replacement.getGenomeRevision(), replacement.evolutionStateKey());
        return transition;
    }

    public boolean preservesOwnerStorageContinuity() {
        if (discarded == null) {
            return false;
        }
        return Objects.equals(discarded.getOwnerId(), replacement.getOwnerId())
                && Objects.equals(discarded.getArtifactStorageKey(), replacement.getArtifactStorageKey());
    }
}
