package obtuseloot.dashboard;

import obtuseloot.analytics.RankAbundanceAnalyzer;
import obtuseloot.analytics.RankAbundanceAnalyzer.RankAbundanceResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DashboardService {
    private static final Pattern SECTION_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*\\{([^}]*)}", Pattern.DOTALL);
    private static final Pattern ENTRY_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    private final RankAbundanceAnalyzer analyzer = new RankAbundanceAnalyzer();
    private final Path analyticsRoot;

    public DashboardService(Path analyticsRoot) {
        this.analyticsRoot = analyticsRoot;
    }

    public Path dashboardRoot() {
        return analyticsRoot.resolve("dashboard");
    }

    public Path dashboardFile() {
        return dashboardRoot().resolve("ecosystem-dashboard.html");
    }

    public DashboardMetrics calculateMetrics() throws IOException {
        String content = Files.readString(analyticsRoot.resolve("ecosystem-balance-data.json"));

        Map<String, Integer> ecosystemMap = parseIntegerMap(content, "familyDistribution");
        Map<String, Integer> branchMap = parseIntegerMap(content, "branchDistribution");
        Map<String, Integer> triggerMap = parseIntegerMap(content, "triggerDiversity");
        Map<String, Integer> mechanicMap = parseIntegerMap(content, "mechanicDiversity");

        Map<String, Integer> lineageMap = deriveLineages(branchMap);
        Map<String, Integer> traitMap = mergeTraits(triggerMap, mechanicMap);

        RankAbundanceResult ecosystem = analyzer.analyze(ecosystemMap);
        RankAbundanceResult lineage = analyzer.analyze(lineageMap);
        RankAbundanceResult branch = analyzer.analyze(branchMap);
        RankAbundanceResult trait = analyzer.analyze(traitMap);

        DashboardMetrics.CollapseRisk collapseRisk = determineRisk(
                ecosystem.dominanceRatio(),
                branch.entropy(),
                lineage.concentration());

        return new DashboardMetrics(
                ecosystem.dominanceRatio(),
                branch.entropy(),
                lineage.concentration(),
                trait.variance(),
                collapseRisk);
    }

    public Path regenerateDashboard() throws IOException {
        DashboardMetrics metrics = calculateMetrics();
        Files.createDirectories(dashboardRoot());
        String html = """
                <!DOCTYPE html>
                <html lang=\"en\">
                <head>
                  <meta charset=\"UTF-8\" />
                  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />
                  <title>ObtuseLoot Ecosystem Dashboard</title>
                  <style>
                    body { font-family: Arial, sans-serif; background: #101522; color: #e8ecf4; margin: 2rem; }
                    .metric { background: #1b2335; border-radius: 8px; padding: 1rem; margin-bottom: 0.75rem; }
                    .label { color: #9ca9c6; font-size: 0.9rem; }
                    .value { font-size: 1.4rem; font-weight: 700; }
                  </style>
                </head>
                <body>
                  <h1>ObtuseLoot Ecosystem Dashboard</h1>
                  <p>Generated from analytics/ecosystem-balance-data.json</p>
                  <div class=\"metric\"><div class=\"label\">Dominance Index</div><div class=\"value\">%s</div></div>
                  <div class=\"metric\"><div class=\"label\">Branch Entropy</div><div class=\"value\">%s</div></div>
                  <div class=\"metric\"><div class=\"label\">Lineage Concentration</div><div class=\"value\">%s</div></div>
                  <div class=\"metric\"><div class=\"label\">Trait Variance</div><div class=\"value\">%s</div></div>
                  <div class=\"metric\"><div class=\"label\">Collapse Risk</div><div class=\"value\">%s</div></div>
                </body>
                </html>
                """.formatted(
                fmt(metrics.dominanceIndex()),
                fmt(metrics.branchEntropy()),
                fmt(metrics.lineageConcentration()),
                fmt(metrics.traitVariance()),
                metrics.collapseRisk().name());
        Files.writeString(dashboardFile(), html);
        return dashboardFile();
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private DashboardMetrics.CollapseRisk determineRisk(double dominance, double entropy, double concentration) {
        int score = 0;
        if (dominance >= 0.40) {
            score++;
        }
        if (entropy <= 2.5) {
            score++;
        }
        if (concentration >= 0.20) {
            score++;
        }
        if (score >= 2) {
            return DashboardMetrics.CollapseRisk.HIGH;
        }
        if (score == 1) {
            return DashboardMetrics.CollapseRisk.MEDIUM;
        }
        return DashboardMetrics.CollapseRisk.LOW;
    }

    private Map<String, Integer> deriveLineages(Map<String, Integer> branchMap) {
        Map<String, Integer> lineage = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : branchMap.entrySet()) {
            String key = entry.getKey();
            String family = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
            lineage.merge(family, entry.getValue(), Integer::sum);
        }
        return lineage;
    }

    private Map<String, Integer> mergeTraits(Map<String, Integer> triggerMap, Map<String, Integer> mechanicMap) {
        Map<String, Integer> traits = new LinkedHashMap<>();
        triggerMap.forEach((k, v) -> traits.put("trigger." + k, v));
        mechanicMap.forEach((k, v) -> traits.put("mechanic." + k, v));
        return traits;
    }

    private Map<String, Integer> parseIntegerMap(String content, String section) {
        Pattern sectionPattern = Pattern.compile(String.format(SECTION_PATTERN_TEMPLATE.pattern(), section), Pattern.DOTALL);
        Matcher sectionMatcher = sectionPattern.matcher(content);
        if (!sectionMatcher.find()) {
            return Map.of();
        }
        String block = sectionMatcher.group(1);
        Matcher entryMatcher = ENTRY_PATTERN.matcher(block);
        Map<String, Integer> result = new LinkedHashMap<>();
        while (entryMatcher.find()) {
            result.put(entryMatcher.group(1), (int) Math.round(Double.parseDouble(entryMatcher.group(2))));
        }
        return result;
    }
}
