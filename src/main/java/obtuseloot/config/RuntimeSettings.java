package obtuseloot.config;

import obtuseloot.names.ArtifactLexemeRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                config.getInt("evolution.advanced-threshold", 45),
                config.getInt("evolution.hybrid-threshold", 70),
                config.getDouble("evolution.archetype-switch-margin", 4.0D),
                config.getDouble("evolution.current-archetype-inertia", 2.0D),
                config.getDouble("drift.base-chance", 0.05D),
                config.getDouble("drift.max-chance", 0.40D),
                config.getDouble("drift.chaos-multiplier", 0.01D),
                config.getDouble("drift.consistency-reduction", 0.005D),
                config.getInt("drift.instability-duration-seconds", 600),
                config.getInt("persistence.autosave-interval-seconds", 300),
                config.getBoolean("naming.use-deterministic-owner-seed", true),
                readLexemePools(config),
                config.getInt("naming.discovery-thresholds.known", 3),
                config.getInt("naming.discovery-thresholds.revealed", 8),
                config.getInt("naming.discovery-thresholds.storied", 14),
                config.getInt("naming.compression.max-words", 6),
                config.getInt("text.channels.name.max-words", 4),
                config.getInt("text.channels.lore.max-words", 14),
                config.getInt("text.channels.identify.max-words", 16),
                config.getInt("text.channels.awakening.max-words", 14),
                config.getInt("text.channels.lineage.max-words", 16),
                config.getInt("text.channels.memory.max-words", 16),
                config.getInt("text.channels.drift.max-words", 10),
                config.getInt("text.channels.convergence.max-words", 14),
                config.getBoolean("runtime.triggerSubscriptionIndexing", true),
                config.getBoolean("runtime.activeArtifactCache", true),
                config.getInt("runtime.activeArtifactCacheMaxEntries", 2048),
                config.getLong("runtime.activeArtifactCacheIdleExpireMs", 300000L)
        );
    }

    private static Map<String, List<String>> readLexemePools(FileConfiguration config) {
        Map<String, List<String>> defaults = ArtifactLexemeRegistry.defaultPools();
        ConfigurationSection section = config.getConfigurationSection("naming.lexeme-pools");
        if (section == null) {
            return defaults;
        }
        Map<String, List<String>> pools = new LinkedHashMap<>(defaults);
        for (String key : section.getKeys(false)) {
            pools.put(key, section.getStringList(key));
        }
        return pools;
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
            int advancedThreshold,
            int hybridThreshold,
            double archetypeSwitchMargin,
            double currentArchetypeInertia,
            double driftBaseChance,
            double driftMaxChance,
            double driftChaosMultiplier,
            double driftConsistencyReduction,
            int driftInstabilityDurationSeconds,
            int autosaveIntervalSeconds,
            boolean namingUseDeterministicOwnerSeed,
            Map<String, List<String>> namingLexemePools,
            int namingDiscoveryKnownThreshold,
            int namingDiscoveryRevealedThreshold,
            int namingDiscoveryStoriedThreshold,
            int namingCompressionMaxWords,
            int textNameMaxWords,
            int textLoreMaxWords,
            int textIdentifyMaxWords,
            int textAwakeningMaxWords,
            int textLineageMaxWords,
            int textMemoryMaxWords,
            int textDriftMaxWords,
            int textConvergenceMaxWords,
            boolean triggerSubscriptionIndexing,
            boolean activeArtifactCache,
            int activeArtifactCacheMaxEntries,
            long activeArtifactCacheIdleExpireMs
    ) {
        private static Snapshot defaults() {
            return new Snapshot(10000L, 6.0D, 12.0D, 8000L, 3, List.of("ENDER_DRAGON", "WITHER", "WARDEN"),
                    300, 0.96D, 120, 10.0D, 10, 25, 45, 70, 4.0D, 2.0D, 0.05D, 0.40D, 0.01D, 0.005D, 600, 300,
                    true, ArtifactLexemeRegistry.defaultPools(),
                    3, 8, 14, 6,
                    4, 14, 16, 14, 16, 16, 10, 14, true, true, 2048, 300000L);
        }
    }
}
