package com.falcoignis.obtuseloot.names;

import com.falcoignis.obtuseloot.config.RuntimeSettings;

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

        List<String> prefixes = NamePoolManager.prefixes();
        List<String> suffixes = NamePoolManager.suffixes();
        List<String> generic = NamePoolManager.generic();

        int prefixSuffixChancePercent = Math.max(0, Math.min(100, settings.namingPrefixSuffixChancePercent()));
        if (random.nextInt(100) < prefixSuffixChancePercent) {
            return prefixes.get(random.nextInt(prefixes.size())) + " " + suffixes.get(random.nextInt(suffixes.size()));
        }

        return generic.get(random.nextInt(generic.size()));
    }
}
