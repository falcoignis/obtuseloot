package obtuseloot.commands;

import obtuseloot.ObtuseLoot;
import obtuseloot.analytics.InteractionHeatmapExporter;
import obtuseloot.analytics.TraitInteractionAnalyzer;
import obtuseloot.analytics.TraitInteractionReportWriter;
import obtuseloot.artifacts.Artifact;
import obtuseloot.dashboard.DashboardMetrics;
import obtuseloot.dashboard.DashboardService;
import obtuseloot.dashboard.DashboardWebServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardCommandExecutor implements CommandExecutor, TabCompleter {
    private final ObtuseLoot plugin;
    private final ObtuseLootCommand delegate;
    private final DashboardService dashboardService;
    private final DashboardWebServer dashboardWebServer;

    public DashboardCommandExecutor(ObtuseLoot plugin,
                                    ObtuseLootCommand delegate,
                                    DashboardService dashboardService,
                                    DashboardWebServer dashboardWebServer) {
        this.plugin = plugin;
        this.delegate = delegate;
        this.dashboardService = dashboardService;
        this.dashboardWebServer = dashboardWebServer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (isDebugDashboardCommand(args)) {
            return handleDebugDashboard(sender);
        }
        if (isEcosystemDashboardCommand(args) || isDashboardCommand(args)) {
            if (!sender.hasPermission("obtuseloot.info")) {
                sender.sendMessage("§cYou do not have permission: obtuseloot.info");
                return true;
            }
            handleDashboardSummary(sender);
            return true;
        }
        if (isEcosystemMapSummary(args)) {
            if (!sender.hasPermission("obtuseloot.info")) {
                sender.sendMessage("§cYou do not have permission: obtuseloot.info");
                return true;
            }
            sender.sendMessage("§dTrait interaction map: §fanalytics/visualizations/trait-interaction-heatmap.png");
            return true;
        }

        return delegate.onCommand(sender, command, label, args);
    }

    private boolean handleDebugDashboard(CommandSender sender) {
        if (!sender.hasPermission("obtuseloot.debug")) {
            sender.sendMessage("§cYou do not have permission: obtuseloot.debug");
            return true;
        }

        try {
            regenerateHeatmapAndReport();
            Path output = dashboardService.regenerateDashboard();
            sender.sendMessage("§aDashboard regenerated at: §f" + output);
            sender.sendMessage("§aHeatmap regenerated at: §fanalytics/visualizations/trait-interaction-heatmap.png");
        } catch (IOException exception) {
            sender.sendMessage("§cFailed to regenerate dashboard: " + exception.getMessage());
        }
        return true;
    }

    private void regenerateHeatmapAndReport() throws IOException {
        List<Artifact> artifacts = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            artifacts.add(plugin.getArtifactManager().getOrCreate(player.getUniqueId()));
        }
        var matrix = new TraitInteractionAnalyzer().analyze(artifacts, plugin.getLineageRegistry().lineages().values());
        new InteractionHeatmapExporter().export(
                matrix,
                Path.of("analytics/visualizations/trait-interaction-heatmap.png"),
                Path.of("analytics/visualizations/trait-interaction-matrix.csv"),
                Path.of("analytics/visualizations/trait-interaction-matrix.json")
        );
        new TraitInteractionReportWriter().write(Path.of("analytics/trait-interaction-report.md"), matrix);
    }

    private void handleDashboardSummary(CommandSender sender) {
        try {
            DashboardMetrics metrics = dashboardService.calculateMetrics();
            Path dashboardPath = dashboardService.dashboardFile();
            String dashboardUrl = dashboardWebServer.isRunning() ? dashboardUrl() : "N/A (web server disabled)";
            String latestSeason = latestSeasonSnapshot();

            if (sender instanceof Player player) {
                player.sendMessage("§d=== ObtuseLoot Ecosystem Health ===");
                player.sendMessage("§7Dominance Index: §f" + format(metrics.dominanceIndex()));
                player.sendMessage("§7Branch Entropy: §f" + format(metrics.branchEntropy()));
                player.sendMessage("§7Trait Variance: §f" + format(metrics.traitVariance()));
                player.sendMessage("§7Lineage Concentration: §f" + format(metrics.lineageConcentration()));
                player.sendMessage("§7Collapse Risk: §f" + metrics.collapseRisk().name());
                player.sendMessage("§7Summary: §f" + "Dominance " + format(metrics.dominanceIndex()) + ", Risk " + metrics.collapseRisk().name());
                player.sendMessage("§8Data scope: generator/ecology aggregate from analytics/ecosystem-balance-data.json.");
                if (dashboardWebServer.isRunning()) {
                    TextComponent link = new TextComponent("§b[View Ecosystem Dashboard]");
                    link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, dashboardUrl));
                    player.spigot().sendMessage(link);
                } else {
                    player.sendMessage("§7Dashboard generated at: §f" + dashboardPath);
                }
                return;
            }

            sender.sendMessage("=== ObtuseLoot Ecosystem Health ===");
            sender.sendMessage("Dominance Index: " + format(metrics.dominanceIndex()));
            sender.sendMessage("Branch Entropy: " + format(metrics.branchEntropy()));
            sender.sendMessage("Trait Variance: " + format(metrics.traitVariance()));
            sender.sendMessage("Lineage Concentration: " + format(metrics.lineageConcentration()));
            sender.sendMessage("Collapse Risk: " + metrics.collapseRisk().name());
            sender.sendMessage("Dashboard file path: " + dashboardPath);
            sender.sendMessage("Dashboard generation succeeded: " + Files.exists(dashboardPath));
            sender.sendMessage("Web endpoint: " + dashboardUrl);
            sender.sendMessage("Latest season snapshot: " + latestSeason);
            sender.sendMessage("Data scope: generator/ecology aggregate from analytics/ecosystem-balance-data.json (not online-player-only telemetry).");
        } catch (IOException exception) {
            sender.sendMessage("§cUnable to read ecosystem analytics: " + exception.getMessage());
        }
    }

    private String latestSeasonSnapshot() {
        for (int season = 3; season >= 1; season--) {
            Path snapshot = Path.of("analytics/world-lab/season" + season + "-ecosystem-dashboard.html");
            if (Files.exists(snapshot)) {
                return snapshot.toString();
            }
        }
        return "not available";
    }

    private String dashboardUrl() {
        String ip = Bukkit.getIp();
        if (ip == null || ip.isBlank()) {
            ip = "localhost";
        }
        return "http://" + ip + ":" + dashboardWebServer.port() + "/ecosystem-dashboard.html";
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private boolean isDashboardCommand(String[] args) {
        return args.length == 1 && "dashboard".equalsIgnoreCase(args[0]);
    }

    private boolean isEcosystemDashboardCommand(String[] args) {
        return (args.length == 1 && "ecosystem".equalsIgnoreCase(args[0]))
                || (args.length == 2 && "ecosystem".equalsIgnoreCase(args[0])
                && ("health".equalsIgnoreCase(args[1]) || "dashboard".equalsIgnoreCase(args[1])));
    }

    private boolean isEcosystemMapSummary(String[] args) {
        return args.length == 2 && "ecosystem".equalsIgnoreCase(args[0]) && "map".equalsIgnoreCase(args[1]);
    }

    private boolean isDebugDashboardCommand(String[] args) {
        return args.length >= 2 && "debug".equalsIgnoreCase(args[0]) && "dashboard".equalsIgnoreCase(args[1]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> extra = new ArrayList<>(delegate.onTabComplete(sender, command, alias, args));
            addIfMissing(extra, "dashboard", args[0]);
            addIfMissing(extra, "ecosystem", args[0]);
            return extra;
        }

        if (args.length == 2 && "ecosystem".equalsIgnoreCase(args[0])) {
            List<String> merged = new ArrayList<>(delegate.onTabComplete(sender, command, alias, args));
            addIfMissing(merged, "health", args[1]);
            addIfMissing(merged, "dashboard", args[1]);
            addIfMissing(merged, "map", args[1]);
            return merged;
        }

        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            List<String> merged = new ArrayList<>(delegate.onTabComplete(sender, command, alias, args));
            addIfMissing(merged, "dashboard", args[1]);
            return merged;
        }

        return delegate.onTabComplete(sender, command, alias, args);
    }

    private void addIfMissing(List<String> values, String candidate, String prefix) {
        if (candidate.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))
                && values.stream().noneMatch(entry -> entry.equalsIgnoreCase(candidate))) {
            values.add(candidate);
        }
    }
}
