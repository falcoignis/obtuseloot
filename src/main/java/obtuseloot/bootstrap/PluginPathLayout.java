package obtuseloot.bootstrap;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class PluginPathLayout {
    private final Path dataRoot;
    private final Path analyticsRoot;

    private PluginPathLayout(Path dataRoot, Path analyticsRoot) {
        this.dataRoot = dataRoot;
        this.analyticsRoot = analyticsRoot;
    }

    public static PluginPathLayout forAnalyticsRoot(Path analyticsRoot) {
        if (analyticsRoot == null) {
            throw new IllegalArgumentException("analyticsRoot must not be null.");
        }
        Path normalized = analyticsRoot.toAbsolutePath().normalize();
        return new PluginPathLayout(normalized, normalized);
    }

    public static PluginPathLayout from(JavaPlugin plugin) {
        Path dataRoot = plugin.getDataFolder().toPath();
        String configuredAnalyticsRoot = plugin.getConfig().getString("paths.analyticsRoot");
        String analyticsRootValue = configuredAnalyticsRoot == null ? "analytics" : configuredAnalyticsRoot.trim();
        if (analyticsRootValue.isBlank()) {
            throw new IllegalStateException("Invalid config value for paths.analyticsRoot: value must not be blank.");
        }
        Path analyticsRoot = Path.of(analyticsRootValue);
        Path resolvedAnalyticsRoot = analyticsRoot.isAbsolute() ? analyticsRoot : dataRoot.resolve(analyticsRoot).normalize();
        return new PluginPathLayout(dataRoot, resolvedAnalyticsRoot);
    }

    public Path dataRoot() {
        return dataRoot;
    }

    public Path analyticsRoot() {
        return analyticsRoot;
    }

    /**
     * Single source of truth for plugin-owned runtime analytics and reporting outputs.
     * <p>
     * Callers must request intent-named paths from this type instead of composing
     * ad-hoc directories or filenames (for example with {@code resolve("telemetry")}).
     */
    public Path dashboardRoot() {
        return analyticsRoot.resolve("dashboard");
    }

    public Path telemetryRoot() {
        return analyticsRoot.resolve("telemetry");
    }

    public Path telemetryArchivePath() {
        return analyticsRoot.resolve("telemetry/ecosystem-events.log");
    }

    public Path telemetrySnapshotPath() {
        return analyticsRoot.resolve("telemetry/rollup-snapshot.properties");
    }

    public Path ecosystemBalanceDataPath() {
        return analyticsRoot.resolve("ecosystem-balance-data.json");
    }

    public Path environmentPressureReportPath() {
        return analyticsRoot.resolve("environment-pressure-report.md");
    }

    public Path triggerSubscriptionReportPath() {
        return analyticsRoot.resolve("performance/trigger-subscription-index-report.md");
    }

    public Path safetyDumpPath() {
        return analyticsRoot.resolve("safety/ecosystem-safety-dump.json");
    }

    public Path traitInteractionHeatmap() {
        return analyticsRoot.resolve("visualizations/trait-interaction-heatmap.png");
    }

    public Path traitInteractionMatrixCsv() {
        return analyticsRoot.resolve("visualizations/trait-interaction-matrix.csv");
    }

    public Path traitInteractionMatrixJson() {
        return analyticsRoot.resolve("visualizations/trait-interaction-matrix.json");
    }

    public Path traitInteractionReport() {
        return analyticsRoot.resolve("trait-interaction-report.md");
    }

    public Path seasonDashboard(int season) {
        return analyticsRoot.resolve("world-lab/season" + season + "-ecosystem-dashboard.html");
    }
}
