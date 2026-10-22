package obtuseloot.abilities.genome;

import obtuseloot.artifacts.Artifact;

/** Immutable attribution within one physical item's history. Identity replacement also changes the seed. */
public record GenomeRevision(long seed, long revision) {
    public GenomeRevision {
        if (seed == 0 || revision < 0) throw new IllegalArgumentException("Invalid genome revision");
    }

    public static GenomeRevision capture(Artifact artifact) {
        return artifact == null || artifact.getArtifactSeed() == 0 ? null
                : new GenomeRevision(artifact.getArtifactSeed(), artifact.getGenomeRevision());
    }

    public boolean matches(Artifact artifact) {
        return artifact != null && seed == artifact.getArtifactSeed() && revision == artifact.getGenomeRevision();
    }
}
