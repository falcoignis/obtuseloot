package obtuseloot.evolution;

import obtuseloot.artifacts.Artifact;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ArtifactUsageTracker {
    private final Map<Long, ArtifactUsageProfile> profiles = new ConcurrentHashMap<>();

    public ArtifactUsageProfile profileFor(Artifact artifact) {
        return profileForSeed(artifact.getArtifactSeed());
    }

    public ArtifactUsageProfile profileForSeed(long artifactSeed) {
        return profiles.computeIfAbsent(artifactSeed, k -> new ArtifactUsageProfile());
    }

    public void trackCreated(Artifact artifact) {
        profileFor(artifact).markCreated(System.currentTimeMillis());
    }

    public void trackUse(Artifact artifact) {
        profileFor(artifact).recordUse(System.currentTimeMillis());
    }

    public void trackKillParticipation(Artifact artifact) {
        profileFor(artifact).recordKill(System.currentTimeMillis());
    }

    public void trackDiscard(Artifact artifact) {
        profileFor(artifact).recordDiscard(System.currentTimeMillis());
    }

    public void trackFusionParticipation(Artifact artifact) {
        profileFor(artifact).recordFusion(System.currentTimeMillis());
    }

    public void trackAwakening(Artifact artifact) {
        profileFor(artifact).recordAwakening(System.currentTimeMillis());
    }
}
