package obtuseloot.dashboard;

import obtuseloot.analytics.EcosystemStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public class DashboardRenderer {
    public Path render(Path output, Map<String, Object> data, String summary) throws IOException {
        Files.createDirectories(output.getParent());

        DashboardMetrics metrics = (DashboardMetrics) data.get("metrics");
        String html = htmlTemplate().formatted(
                summary,
                fmt(metrics.dominanceIndex()),
                fmt(metrics.branchEntropy()),
                fmt(metrics.traitVariance()),
                fmt(metrics.lineageConcentration()),
                metrics.collapseRisk().name(),
                fmt(metrics.endArtifacts()),
                fmt(metrics.latestTnt()),
                statusBadge(metrics.ecosystemStatus()),
                renderMap((Map<String, Integer>) data.get("archetypes")),
                data.get("heatmapImage"),
                renderMap((Map<String, Integer>) data.get("traits")),
                renderMap((Map<String, Integer>) data.get("lineages")),
                renderSpeciesNiche((Map<String, Object>) data.get("speciesNicheMetrics")),
                renderEcosystemHealth(metrics)
        );

        Files.writeString(output, html);
        return output;
    }

    private String htmlTemplate() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset=\"UTF-8\">
                  <title>ObtuseLoot Ecosystem Dashboard</title>
                  <style>
                    body{font-family:Arial;background:#0f1524;color:#eaf0ff;margin:20px}
                    .grid{display:grid;grid-template-columns:repeat(2,minmax(360px,1fr));gap:14px}
                    .panel{background:#1a243b;border-radius:10px;padding:12px}
                    .strip{display:grid;grid-template-columns:repeat(8,1fr);gap:8px}
                    .metric{background:#25304d;padding:10px;border-radius:8px}
                    .small{color:#9bb1de;font-size:12px}
                    pre{white-space:pre-wrap}
                    .badge{padding:2px 8px;border-radius:999px;font-weight:bold}
                    .green{background:#1d7f49;color:#d7ffe7}
                    .yellow{background:#957500;color:#fff6cc}
                    .red{background:#962c2c;color:#ffd7d7}
                  </style>
                </head>
                <body>
                  <h1>ObtuseLoot Ecosystem Dashboard</h1>
                  <p class=\"small\">%s</p>
                  <div class=\"strip\">
                    <div class=\"metric\"><div class=\"small\">Dominance Index</div><b>%s</b></div>
                    <div class=\"metric\"><div class=\"small\">Branch Entropy</div><b>%s</b></div>
                    <div class=\"metric\"><div class=\"small\">Trait Variance</div><b>%s</b></div>
                    <div class=\"metric\"><div class=\"small\">Lineage Concentration</div><b>%s</b></div>
                    <div class=\"metric\"><div class=\"small\">Collapse Risk</div><b>%s</b></div>
                    <div class=\"metric\"><div class=\"small\">Effective Niches (END)</div><b>%s</b></div>
                    <div class=\"metric\"><div class=\"small\">Temporal Niche Turnover (TNT)</div><b>%s</b></div>
                    <div class=\"metric\"><div class=\"small\">Ecosystem Status</div><b>%s</b></div>
                  </div>
                  <div class=\"grid\">
                    <div class=\"panel\"><h3>Rank-Abundance Curve</h3><pre>%s</pre></div>
                    <div class=\"panel\"><h3>Trait Interaction Heatmap</h3><img src=\"%s\" style=\"max-width:100%%\"></div>
                    <div class=\"panel\"><h3>Genome Trait Scatter Plot</h3><pre>%s</pre></div>
                    <div class=\"panel\"><h3>Lineage Survival / Concentration</h3><pre>%s</pre></div>
                    <div class=\"panel\"><h3>Species & Niche Health</h3><pre>%s</pre></div>
                    <div class=\"panel\"><h3>Ecosystem Health Gauge</h3><pre>%s</pre></div>
                  </div>
                </body>
                </html>
                """;
    }

    private String renderMap(Map<String, Integer> data) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        return builder.toString();
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private String renderSpeciesNiche(Map<String, Object> speciesNicheMetrics) {
        if (speciesNicheMetrics == null || speciesNicheMetrics.isEmpty()) {
            return "Species/niche analytics not generated yet.";
        }
        return "activeSpecies=" + speciesNicheMetrics.getOrDefault("activeSpecies", 0)
                + "\ndominantSpeciesShare=" + speciesNicheMetrics.getOrDefault("dominantSpeciesShare", 0)
                + "\nnicheCount=" + speciesNicheMetrics.getOrDefault("nicheCount", 0)
                + "\nspeciesPerNiche=" + speciesNicheMetrics.getOrDefault("speciesPerNiche", Map.of())
                + "\nnicheTurnover=" + speciesNicheMetrics.getOrDefault("nicheTurnover", 0)
                + "\ndominantNicheShare=" + speciesNicheMetrics.getOrDefault("dominantNicheShare", 0)
                + "\novercrowdedNicheCount=" + speciesNicheMetrics.getOrDefault("overcrowdedNicheCount", 0);
    }

    private String renderEcosystemHealth(DashboardMetrics metrics) {
        return "END_artifacts=" + fmt(metrics.endArtifacts())
                + "\nEND_species=" + (metrics.endSpecies() == null ? "N/A" : fmt(metrics.endSpecies()))
                + "\nTNT_latest=" + fmt(metrics.latestTnt())
                + "\nEND_trend=" + metrics.endTrend()
                + "\nTNT_trend=" + metrics.tntTrend()
                + "\nstatus=" + metrics.ecosystemStatus();
    }

    private String statusBadge(EcosystemStatus status) {
        String colorClass = switch (status) {
            case HEALTHY_ECOSYSTEM -> "green";
            case EARLY_DIVERGENCE, TURBULENT, STAGNANT -> "yellow";
            case COLLAPSED, FRAGMENTED -> "red";
        };
        return "<span class=\"badge " + colorClass + "\">" + status.name() + "</span>";
    }
}
