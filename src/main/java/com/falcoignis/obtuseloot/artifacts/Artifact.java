package com.falcoignis.obtuseloot.artifacts;

import java.util.UUID;

public class Artifact {
    private final UUID ownerId;
    private final long seed;
    private String evolutionPath = "base";
    private String awakeningPath = "dormant";

    public Artifact(UUID ownerId) {
        this.ownerId = ownerId;
        this.seed = Math.abs(ownerId.getMostSignificantBits() ^ ownerId.getLeastSignificantBits());
    }

    public UUID getOwnerId() { return ownerId; }
    public long getSeed() { return seed; }
    public String getEvolutionPath() { return evolutionPath; }
    public void setEvolutionPath(String evolutionPath) { this.evolutionPath = evolutionPath; }
    public String getAwakeningPath() { return awakeningPath; }
    public void setAwakeningPath(String awakeningPath) { this.awakeningPath = awakeningPath; }
}
