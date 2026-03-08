package obtuseloot.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

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
                config.getInt("evolution.archetype-score", 20),
                config.getInt("evolution.archetype-dominance-delta", 8),
                config.getInt("evolution.tempered-score", 40),
                config.getInt("evolution.mythic-score", 120),
                config.getInt("evolution.hybrid-score", 250),
                config.getInt("fusion.min-score", 320),
                loadFusionRecipes(config.getConfigurationSection("fusion.recipes")),
                config.getDouble("drift.base-chance", 0.03D),
                config.getDouble("drift.chaos-multiplier", 0.01D),
                config.getDouble("drift.consistency-reduction", 0.005D),
                config.getDouble("drift.max-chance", 0.60D),
                config.getLong("lore.min-update-interval-ms", 500L),
                config.getInt("naming.prefix-suffix-chance-percent", 60),
                config.getBoolean("naming.use-deterministic-owner-seed", true),
                config.getInt("awakening.ravager.min-brutality", 140),
                config.getInt("awakening.ravager.min-boss-kills", 1),
                config.getInt("awakening.deadeye.min-precision", 120),
                config.getInt("awakening.deadeye.min-mobility", 80),
                config.getInt("awakening.vanguard.min-survival", 140),
                config.getInt("awakening.vanguard.min-consistency", 100),
                config.getInt("awakening.strider.min-mobility", 140),
                config.getInt("awakening.strider.min-precision", 100),
                config.getInt("awakening.harbinger.min-chaos", 120),
                config.getInt("awakening.harbinger.min-boss-kills", 2),
                config.getInt("awakening.warden.min-survival", 120),
                config.getInt("awakening.warden.min-consistency", 100),
                config.getInt("awakening.paragon.min-score", 500),
                config.getInt("awakening.paragon.min-consistency", 120)
        );
    }

    private static List<FusionRecipe> loadFusionRecipes(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }

        List<FusionRecipe> recipes = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection recipeSection = section.getConfigurationSection(id);
            if (recipeSection == null) {
                continue;
            }

            recipes.add(new FusionRecipe(
                    id,
                    recipeSection.getString("archetype", "paragon"),
                    recipeSection.getInt("min-precision", 0),
                    recipeSection.getInt("min-brutality", 0),
                    recipeSection.getInt("min-survival", 0),
                    recipeSection.getInt("min-mobility", 0),
                    recipeSection.getInt("min-chaos", 0),
                    recipeSection.getInt("min-consistency", 0),
                    recipeSection.getInt("min-boss-kills", 0)
            ));
        }

        return List.copyOf(recipes);
    }

    public static Snapshot get() {
        return current;
    }

    public record Snapshot(
            double precisionThresholdDamage,
            int archetypeScore,
            int archetypeDominanceDelta,
            int temperedScore,
            int mythicScore,
            int hybridScore,
            int fusionMinScore,
            List<FusionRecipe> fusionRecipes,
            double driftBaseChance,
            double driftChaosMultiplier,
            double driftConsistencyReduction,
            double driftMaxChance,
            long loreMinUpdateIntervalMs,
            int namingPrefixSuffixChancePercent,
            boolean namingUseDeterministicOwnerSeed,
            int ravagerMinBrutality,
            int ravagerMinBossKills,
            int deadeyeMinPrecision,
            int deadeyeMinMobility,
            int vanguardMinSurvival,
            int vanguardMinConsistency,
            int striderMinMobility,
            int striderMinPrecision,
            int harbingerMinChaos,
            int harbingerMinBossKills,
            int wardenMinSurvival,
            int wardenMinConsistency,
            int paragonMinScore,
            int paragonMinConsistency
    ) {
        private static Snapshot defaults() {
            return new Snapshot(
                    10.0D,
                    20,
                    8,
                    40,
                    120,
                    250,
                    320,
                    List.of(),
                    0.03D,
                    0.01D,
                    0.005D,
                    0.60D,
                    500L,
                    60,
                    true,
                    140,
                    1,
                    120,
                    80,
                    140,
                    100,
                    140,
                    100,
                    120,
                    2,
                    120,
                    100,
                    500,
                    120
            );
        }
    }

    public record FusionRecipe(
            String id,
            String archetype,
            int minPrecision,
            int minBrutality,
            int minSurvival,
            int minMobility,
            int minChaos,
            int minConsistency,
            int minBossKills
    ) {
    }
}
