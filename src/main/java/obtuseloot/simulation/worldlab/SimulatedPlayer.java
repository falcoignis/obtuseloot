package obtuseloot.simulation.worldlab;

import java.util.List;
import java.util.UUID;

public record SimulatedPlayer(
        UUID id,
        PlayerBehaviorModel behaviorModel,
        BehaviorProfile profile,
        List<SimulatedArtifactAgent> artifacts
) {
    public record BehaviorProfile(
            double aggression,
            double precision,
            double mobility,
            double survival,
            double chaos,
            double bossSeeking,
            double sessionLength,
            double riskTolerance,
            double consistency
    ) {
    }
}
