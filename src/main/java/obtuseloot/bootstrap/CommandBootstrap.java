package obtuseloot.bootstrap;

import obtuseloot.ObtuseLoot;
import obtuseloot.commands.DashboardCommandExecutor;
import obtuseloot.commands.ObtuseLootCommand;
import obtuseloot.dashboard.DashboardService;
import obtuseloot.dashboard.DashboardWebServer;

public final class CommandBootstrap {
    private CommandBootstrap() {
    }

    public static void register(ObtuseLoot plugin,
                                DashboardService dashboardService,
                                DashboardWebServer dashboardWebServer,
                                PluginPathLayout paths) {
        if (plugin.getCommand("obtuseloot") == null) {
            return;
        }
        ObtuseLootCommand command = new ObtuseLootCommand(plugin);
        DashboardCommandExecutor dashboardCommandExecutor = new DashboardCommandExecutor(plugin, command,
                dashboardService, dashboardWebServer, paths);
        plugin.getCommand("obtuseloot").setExecutor(dashboardCommandExecutor);
        plugin.getCommand("obtuseloot").setTabCompleter(dashboardCommandExecutor);
    }
}
