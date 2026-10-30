package obtuseloot.artifacts;

import java.util.Objects;
import java.util.UUID;

/** An owned item's remembered position; reading it never loads a world or chunk. */
public record ArtifactWaypoint(UUID world, double x, double y, double z) {
    public ArtifactWaypoint {
        Objects.requireNonNull(world);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("Invalid waypoint position");
    }
}
