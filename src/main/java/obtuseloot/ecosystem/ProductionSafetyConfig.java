package obtuseloot.ecosystem;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configurable parameters for production safety guards.
 * Loaded from config.yml under the {@code safety.*} section.
 * Defaults match safe current behaviour — no suppression fires under normal distributions.
 */
public record ProductionSafetyConfig(
        /** Category share above which the category weight is suppressed (0.65 = 65%). */
        double categoryDominanceThreshold,
        /** Multiplicative suppression applied to a dominant category's final weight (0.85 = 15% reduction). */
        double categorySuppressionFactor,
        /** Template share above which the template weight is suppressed (0.55 = 55%). */
        double templateDominanceThreshold,
        /** Multiplicative suppression applied to a dominant template's final weight (0.90 = 10% reduction). */
        double templateSuppressionFactor,
        /** Candidate pool size below which pool collapse is declared and eligibility relaxation triggers. */
        int candidatePoolCollapseThreshold,
        /** Number of distribution snapshots in the rolling evaluation window. */
        int rollingWindowSize,
        /** When true, emits a logger warning each time a guard threshold is crossed. */
        boolean telemetryVerbose,
        /** How many evaluation events between automatic snapshot captures (0 = disabled). */
        int snapshotIntervalEvents,
        /** When true, logs a safety summary to console every {@code periodicConsoleLogIntervalTicks} ticks. */
        boolean periodicConsoleLog,
        /** Tick interval for periodic console logging when {@code periodicConsoleLog} is enabled. */
        int periodicConsoleLogIntervalTicks
) {

    public static ProductionSafetyConfig defaults() {
        return new ProductionSafetyConfig(
                0.65, 0.85,
                0.55, 0.90,
                2,
                100,
                false,
                500,
                false,
                1200
        );
    }

    public static ProductionSafetyConfig from(FileConfiguration config) {
        ProductionSafetyConfig d = defaults();
        return new ProductionSafetyConfig(
                config.getDouble("safety.guards.categoryDominanceThreshold", d.categoryDominanceThreshold()),
                config.getDouble("safety.guards.categorySuppressionFactor", d.categorySuppressionFactor()),
                config.getDouble("safety.guards.templateDominanceThreshold", d.templateDominanceThreshold()),
                config.getDouble("safety.guards.templateSuppressionFactor", d.templateSuppressionFactor()),
                config.getInt("safety.guards.candidatePoolCollapseThreshold", d.candidatePoolCollapseThreshold()),
                config.getInt("safety.guards.rollingWindowSize", d.rollingWindowSize()),
                config.getBoolean("safety.telemetryVerbose", d.telemetryVerbose()),
                config.getInt("safety.snapshotIntervalEvents", d.snapshotIntervalEvents()),
                config.getBoolean("safety.periodicConsoleLog", d.periodicConsoleLog()),
                config.getInt("safety.periodicConsoleLogIntervalTicks", d.periodicConsoleLogIntervalTicks())
        );
    }
}
