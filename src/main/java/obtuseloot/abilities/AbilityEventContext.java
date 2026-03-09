package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;

public record AbilityEventContext(AbilityTrigger trigger, Artifact artifact, ArtifactReputation reputation,
                                  double value, String source) {
}
