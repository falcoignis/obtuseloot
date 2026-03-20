package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.AbilityProfile;
import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;

public class SimulatedArtifactAgent {
    private Artifact artifact;
    private final ArtifactReputation reputation;
    private AbilityProfile abilityProfile;
    private final EvolutionaryAbilityRuntimeState evolutionaryAbilityState = new EvolutionaryAbilityRuntimeState();

    public SimulatedArtifactAgent(Artifact artifact) {
        this.artifact = artifact;
        this.reputation = new ArtifactReputation();
    }

    public Artifact artifact() {
        return artifact;
    }

    public void replaceArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public ArtifactReputation reputation() {
        return reputation;
    }

    public AbilityProfile abilityProfile() {
        return abilityProfile;
    }

    public void setAbilityProfile(AbilityProfile abilityProfile) {
        this.abilityProfile = abilityProfile;
    }

    public EvolutionaryAbilityRuntimeState evolutionaryAbilityState() {
        return evolutionaryAbilityState;
    }
}
