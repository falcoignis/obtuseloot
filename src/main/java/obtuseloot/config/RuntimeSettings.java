package obtuseloot.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public final class RuntimeSettings {
    private static volatile Snapshot current = Snapshot.defaults();

    private RuntimeSettings() {
    }

    public static void load(FileConfiguration config) {
        current = new Snapshot(
                config.getLong("reputation.combat-window-ms", 10000L),
                config.getDouble("reputation.low-health-threshold", 6.0D),
                config.getDouble("reputation.mobility-distance-threshold", 12.0D),
                config.getLong("reputation.kill-chain-window-ms", 8000L),
                config.getInt("reputation.multi-target-chaos-threshold", 3),
                config.getStringList("reputation.boss-types"),
                config.getInt("reputation.volatile-decay-interval-seconds", 300),
                config.getDouble("reputation.volatile-decay-factor", 0.96D),
                config.getInt("reputation.context-cleanup-seconds", 120),
                config.getDouble("combat.precision-threshold-damage", 10.0D),

                config.getInt("evolution.archetype-threshold", 10),
                config.getInt("evolution.tempered-threshold", 25),
                config.getInt("evolution.mythic-threshold", 45),
                config.getInt("evolution.hybrid-threshold", 70),
                config.getDouble("evolution.archetype-switch-margin", 4.0D),
                config.getDouble("evolution.current-archetype-inertia", 2.0D),

                config.getDouble("drift.base-chance", 0.05D),
                config.getDouble("drift.max-chance", 0.40D),
                config.getDouble("drift.chaos-multiplier", 0.01D),
                config.getDouble("drift.consistency-reduction", 0.005D),
                config.getInt("drift.instability-duration-seconds", 600),

                config.getInt("persistence.autosave-interval-seconds", 300),
                config.getInt("naming.prefix-suffix-chance-percent", 60),
                config.getBoolean("naming.use-deterministic-owner-seed", true)
        );
    }

    public static Snapshot get() { return current; }

    public record Snapshot(
            long combatWindowMs,
            double lowHealthThreshold,
            double mobilityDistanceThreshold,
            long killChainWindowMs,
            int multiTargetChaosThreshold,
            List<String> bossTypes,
            int volatileDecayIntervalSeconds,
            double volatileDecayFactor,
            int contextCleanupSeconds,
            double precisionThresholdDamage,
            int archetypeThreshold,
            int temperedThreshold,
            int mythicThreshold,
            int hybridThreshold,
            double archetypeSwitchMargin,
            double currentArchetypeInertia,
            double driftBaseChance,
            double driftMaxChance,
            double driftChaosMultiplier,
            double driftConsistencyReduction,
            int driftInstabilityDurationSeconds,
            int autosaveIntervalSeconds,
            int namingPrefixSuffixChancePercent,
            boolean namingUseDeterministicOwnerSeed
    ) {
        private static Snapshot defaults() {
            return new Snapshot(10000L, 6.0D, 12.0D, 8000L, 3, List.of("ENDER_DRAGON", "WITHER", "WARDEN"),
                    300, 0.96D, 120, 10.0D, 10, 25, 45, 70, 4.0D, 2.0D, 0.05D, 0.40D, 0.01D, 0.005D, 600, 300,
                    60, true);
        }
    }
}
