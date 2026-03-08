package com.falcoignis.obtuseloot.artifacts;

import java.util.UUID;

public class Artifact {
    private final UUID owner;
    private String evolutionStage;
    private boolean awakened;

    public Artifact(UUID owner) {
        this.owner = owner;
        this.evolutionStage = "base";
        this.awakened = false;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getEvolutionStage() {
        return evolutionStage;
    }

    public void setEvolutionStage(String evolutionStage) {
        this.evolutionStage = evolutionStage;
    }

    public boolean isAwakened() {
        return awakened;
    }

    public void setAwakened(boolean awakened) {
        this.awakened = awakened;
    }
}
