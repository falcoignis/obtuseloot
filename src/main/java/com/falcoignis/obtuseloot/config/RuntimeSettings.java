package com.falcoignis.obtuseloot.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Centralized runtime configuration for balancing and event throttling.
 *
 * <p>Values are loaded from {@code config.yml} at startup/reload and read from hot paths
 * (combat and kill listeners), so this class keeps access lock-free by swapping immutable snapshots.
 */
public final class RuntimeSettings {
    private static volatile Snapshot current = Snapshot.defaults();

    private RuntimeSettings() {
    }

    public static void load(FileConfiguration config) {
        current = new Snapshot(
                config.getDouble("combat.precision-threshold-damage", 10.0D),
                config.getInt("evolution.tempered-score", 40),
                config.getInt("evolution.mythic-score", 120),
                config.getInt("evolution.hybrid-score", 250),
                config.getDouble("drift.base-chance", 0.03D),
                config.getDouble("drift.chaos-multiplier", 0.01D),
                config.getDouble("drift.consistency-reduction", 0.005D),
                config.getDouble("drift.max-chance", 0.60D),
                config.getLong("lore.min-update-interval-ms", 500L),
                config.getInt("naming.prefix-suffix-chance-percent", 60),
                config.getBoolean("naming.use-deterministic-owner-seed", true),
                config.getInt("awakening.executioners-oath.min-score", 500),
                config.getInt("awakening.executioners-oath.min-boss-kills", 1),
                config.getInt("awakening.stormblade.min-precision", 120),
                config.getInt("awakening.stormblade.min-mobility", 80),
                config.getInt("awakening.last-survivor.min-survival", 120),
                config.getInt("awakening.last-survivor.min-consistency", 100)
        );
    }

    public static Snapshot get() {
        return current;
    }

    public record Snapshot(
            double precisionThresholdDamage,
            int temperedScore,
            int mythicScore,
            int hybridScore,
            double driftBaseChance,
            double driftChaosMultiplier,
            double driftConsistencyReduction,
            double driftMaxChance,
            long loreMinUpdateIntervalMs,
            int namingPrefixSuffixChancePercent,
            boolean namingUseDeterministicOwnerSeed,
            int executionersOathMinScore,
            int executionersOathMinBossKills,
            int stormbladeMinPrecision,
            int stormbladeMinMobility,
            int lastSurvivorMinSurvival,
            int lastSurvivorMinConsistency
    ) {
        private static Snapshot defaults() {
            return new Snapshot(
                    10.0D,
                    40,
                    120,
                    250,
                    0.03D,
                    0.01D,
                    0.005D,
                    0.60D,
                    500L,
                    60,
                    true,
                    500,
                    1,
                    120,
                    80,
                    120,
                    100
            );
        }
    }
}
