package obtuseloot.bootstrap;

import obtuseloot.ObtuseLoot;
import obtuseloot.dashboard.DashboardService;
import obtuseloot.dashboard.DashboardWebServer;

public final class DashboardBootstrap {
    private DashboardBootstrap() {
    }

    public static Result initialize(ObtuseLoot plugin, PluginPathLayout paths) {
        DashboardService dashboardService = new DashboardService(paths.analyticsRoot());
        int dashboardPort = plugin.getConfig().getInt("dashboard.port", 8085);
        boolean dashboardWebEnabled = plugin.getConfig().getBoolean("dashboard.webServerEnabled", false);
        DashboardWebServer dashboardWebServer = new DashboardWebServer(dashboardService.dashboardRoot(), dashboardPort);

        try {
            dashboardService.regenerateDashboard();
            if (dashboardWebEnabled) {
                dashboardWebServer.start();
                plugin.getLogger().info("[Dashboard] Serving ecosystem dashboard at http://localhost:" + dashboardPort + "/ecosystem-dashboard.html");
            } else {
                plugin.getLogger().info("[Dashboard] Web server disabled; dashboard generated to " + dashboardService.dashboardRoot() + ".");
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[Dashboard] Failed to initialize dashboard: " + exception.getMessage());
        }
        return new Result(dashboardService, dashboardWebServer);
    }

    public record Result(DashboardService dashboardService, DashboardWebServer dashboardWebServer) {
    }
}
