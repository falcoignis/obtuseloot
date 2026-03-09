package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;

public interface AbilityResolver {
    AbilityProfile resolve(Artifact artifact, ArtifactReputation reputation);
}
