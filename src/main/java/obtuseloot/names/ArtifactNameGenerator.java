package obtuseloot.names;

import obtuseloot.config.RuntimeSettings;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Display-name generator backed by configurable runtime settings and persisted name pools.
 */
public final class ArtifactNameGenerator {
    private ArtifactNameGenerator() {
    }

    public static String generate(UUID ownerId) {
        RuntimeSettings.Snapshot settings = RuntimeSettings.get();
        Random random = settings.namingUseDeterministicOwnerSeed()
                ? new Random(Math.abs(ownerId.getMostSignificantBits() ^ ownerId.getLeastSignificantBits()))
                : ThreadLocalRandom.current();
        return generateWithRandom(random, settings);
    }

    public static String generateFromSeed(long artifactSeed) {
        return generateWithRandom(new Random(artifactSeed ^ 0x9E3779B97F4A7C15L), RuntimeSettings.get());
    }

    private static String generateWithRandom(Random random, RuntimeSettings.Snapshot settings) {
        List<String> prefixes = NamePoolManager.prefixes();
        List<String> suffixes = NamePoolManager.suffixes();
        List<String> generic = NamePoolManager.generic();

        if (prefixes.isEmpty() || suffixes.isEmpty()) {
            if (!generic.isEmpty()) {
                return generic.get(random.nextInt(generic.size()));
            }
            return "Nameless Artifact";
        }

        int prefixSuffixChancePercent = Math.max(0, Math.min(100, settings.namingPrefixSuffixChancePercent()));
        if (random.nextInt(100) < prefixSuffixChancePercent || generic.isEmpty()) {
            return prefixes.get(random.nextInt(prefixes.size())) + " " + suffixes.get(random.nextInt(suffixes.size()));
        }

        return generic.get(random.nextInt(generic.size()));
    }
}
