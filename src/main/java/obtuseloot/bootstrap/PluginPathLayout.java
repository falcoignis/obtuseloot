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

    public static PluginPathLayout from(JavaPlugin plugin) {
        Path dataRoot = plugin.getDataFolder().toPath();
        String configuredAnalyticsRoot = plugin.getConfig().getString("paths.analyticsRoot", "analytics");
        Path analyticsRoot = Path.of(configuredAnalyticsRoot == null || configuredAnalyticsRoot.isBlank() ? "analytics" : configuredAnalyticsRoot);
        Path resolvedAnalyticsRoot = analyticsRoot.isAbsolute() ? analyticsRoot : dataRoot.resolve(analyticsRoot).normalize();
        return new PluginPathLayout(dataRoot, resolvedAnalyticsRoot);
    }

    public Path dataRoot() {
        return dataRoot;
    }

    public Path analyticsRoot() {
        return analyticsRoot;
    }

    public Path dashboardRoot() {
        return analyticsRoot.resolve("dashboard");
    }

    public Path telemetryArchive() {
        return analyticsRoot.resolve("telemetry/ecosystem-events.log");
    }

    public Path telemetryRollupSnapshot() {
        return analyticsRoot.resolve("telemetry/rollup-snapshot.properties");
    }

    public Path environmentPressureReport() {
        return analyticsRoot.resolve("environment-pressure-report.md");
    }

    public Path triggerSubscriptionReport() {
        return analyticsRoot.resolve("performance/trigger-subscription-index-report.md");
    }

    public Path safetyDump() {
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
