package com.falcoignis.obtuseloot.names;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Deterministic display-name generator so the large default name pools are actually used.
 */
public final class ArtifactNameGenerator {
    private ArtifactNameGenerator() {
    }

    public static String generate(UUID ownerId) {
        long seed = Math.abs(ownerId.getMostSignificantBits() ^ ownerId.getLeastSignificantBits());
        Random random = new Random(seed);

        List<String> prefixes = Prefixes.get();
        List<String> suffixes = Suffixes.get();
        List<String> generic = Generic.get();

        int roll = random.nextInt(100);
        if (roll < 60) {
            return prefixes.get(random.nextInt(prefixes.size())) + " " + suffixes.get(random.nextInt(suffixes.size()));
        }

        return generic.get(random.nextInt(generic.size()));
    }
}
