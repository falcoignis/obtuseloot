package obtuseloot.ecology;

import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.persistence.PlayerStateStore;
import java.util.Random;
import java.util.UUID;

/** Reuses the production test seam without exposing controlled identities in the plugin API. */
public final class ReplayEcosystemFixture {
    private ReplayEcosystemFixture() { }
    public static LocalEcosystem create(PlayerStateStore store, ArtifactManager manager, long seed) {
        var identities = new Random(seed ^ 0x4944454E54495459L);
        return new LocalEcosystem(store, manager, new LocalInheritancePool(),
                () -> "artifact:" + new UUID(identities.nextLong(), identities.nextLong()));
    }
}
