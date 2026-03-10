package obtuseloot.artifacts.cache;

import obtuseloot.artifacts.Artifact;

public record ActiveArtifactHandle(ArtifactCacheKey key, Artifact artifact, boolean cacheHit) {
}
