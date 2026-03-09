package obtuseloot.dashboard;

import obtuseloot.analytics.RankAbundanceAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DashboardDataAssembler {
    private static final Pattern ENTRY_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    private final RankAbundanceAnalyzer rankAbundanceAnalyzer = new RankAbundanceAnalyzer();

    public Map<String, Object> assemble(Path analyticsRoot, DashboardMetrics metrics) throws IOException {
        String content = Files.readString(analyticsRoot.resolve("ecosystem-balance-data.json"));
        Map<String, Integer> archetypes = parseIntegerMap(content, "familyDistribution");
        Map<String, Integer> branches = parseIntegerMap(content, "branchDistribution");
        Map<String, Integer> lineages = deriveLineages(branches);
        Map<String, Integer> traits = parseIntegerMap(content, "triggerDiversity");
        traits.putAll(parseIntegerMap(content, "mechanicDiversity"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("metrics", metrics);
        data.put("archetypes", archetypes);
        data.put("branches", branches);
        data.put("lineages", lineages);
        data.put("traits", traits);
        data.put("rank", rankAbundanceAnalyzer.analyze(archetypes).rankedAbundance());
        data.put("heatmapImage", "../visualizations/trait-interaction-heatmap.png");
        data.put("heatmapJson", "../visualizations/trait-interaction-matrix.json");
        data.put("collapseMetrics", parseCollapse(analyticsRoot.resolve("ecosystem-health-report.md")));
        return data;
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

    private Map<String, Double> parseCollapse(Path reportPath) throws IOException {
        Map<String, Double> out = new LinkedHashMap<>();
        if (!Files.exists(reportPath)) {
            return out;
        }
        String content = Files.readString(reportPath).toLowerCase();
        out.put("collapseSignal", content.contains("high") ? 0.9 : (content.contains("medium") ? 0.5 : 0.2));
        return out;
    }

    private Map<String, Integer> parseIntegerMap(String content, String section) {
        Pattern sectionPattern = Pattern.compile("\"" + Pattern.quote(section) + "\"\\s*:\\s*\\{([^}]*)}", Pattern.DOTALL);
        Matcher sectionMatcher = sectionPattern.matcher(content);
        if (!sectionMatcher.find()) {
            return new LinkedHashMap<>();
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
