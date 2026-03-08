package com.falcoignis.obtuseloot.artifacts;

import java.util.UUID;

public class Artifact {
    private final UUID ownerId;
    private final long seed;
    private final String name;
    private String archetypePath = "unformed";
    private String evolutionPath = "base";
    private String fusionPath = "none";
    private String awakeningPath = "dormant";

    public Artifact(UUID ownerId, String name) {
        this.ownerId = ownerId;
        this.name = name;
        this.seed = Math.abs(ownerId.getMostSignificantBits() ^ ownerId.getLeastSignificantBits());
    }

    public UUID getOwnerId() { return ownerId; }
    public long getSeed() { return seed; }
    public String getName() { return name; }
    public String getArchetypePath() { return archetypePath; }
    public void setArchetypePath(String archetypePath) { this.archetypePath = archetypePath; }
    public String getEvolutionPath() { return evolutionPath; }
    public void setEvolutionPath(String evolutionPath) { this.evolutionPath = evolutionPath; }
    public String getFusionPath() { return fusionPath; }
    public void setFusionPath(String fusionPath) { this.fusionPath = fusionPath; }
    public String getAwakeningPath() { return awakeningPath; }
    public void setAwakeningPath(String awakeningPath) { this.awakeningPath = awakeningPath; }
}
