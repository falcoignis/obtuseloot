package obtuseloot.analytics;

import obtuseloot.analytics.RankAbundanceAnalyzer.RankAbundanceResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualizationExporter {
    private static final Pattern SECTION_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*\\{([^}]*)}", Pattern.DOTALL);
    private static final Pattern ENTRY_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    private final RankAbundanceAnalyzer analyzer = new RankAbundanceAnalyzer();
    private final EcosystemHealthVisualizer visualizer = new EcosystemHealthVisualizer();

    public static void main(String[] args) throws IOException {
        new VisualizationExporter().generateArtifacts();
    }

    public void generateArtifacts() throws IOException {
        Path source = Path.of("analytics/ecosystem-balance-data.json");
        String content = Files.readString(source);

        Map<String, Integer> ecosystemMap = parseIntegerMap(content, "familyDistribution");
        Map<String, Integer> branchMap = parseIntegerMap(content, "branchDistribution");
        Map<String, Integer> triggerMap = parseIntegerMap(content, "triggerDiversity");
        Map<String, Integer> mechanicMap = parseIntegerMap(content, "mechanicDiversity");

        Map<String, Integer> lineageMap = deriveLineages(branchMap);
        Map<String, Integer> traitMap = mergeTraits(triggerMap, mechanicMap);

        RankAbundanceResult ecosystem = analyzer.analyze(ecosystemMap);
        RankAbundanceResult lineage = analyzer.analyze(lineageMap);
        RankAbundanceResult trait = analyzer.analyze(traitMap);
        RankAbundanceResult branch = analyzer.analyze(branchMap);

        visualizer.createRankAbundanceChart("Ecosystem Rank-Abundance", ecosystem.rankedAbundance(),
                Path.of("analytics/visualizations/ecosystem-rank-abundance.png"));
        visualizer.createRankAbundanceChart("Lineage Rank-Abundance", lineage.rankedAbundance(),
                Path.of("analytics/visualizations/lineage-rank-abundance.png"));
        visualizer.createRankAbundanceChart("Trait Rank-Abundance", trait.rankedAbundance(),
                Path.of("analytics/visualizations/trait-rank-abundance.png"));

        String report = buildReport(ecosystem, branch, trait, lineage);
        Files.writeString(Path.of("analytics/ecosystem-health-report.md"), report);
    }

    private String buildReport(RankAbundanceResult ecosystem,
                               RankAbundanceResult branch,
                               RankAbundanceResult trait,
                               RankAbundanceResult lineage) {
        return "# Ecosystem Health Report\n\n"
                + "## Metrics\n"
                + "- ecosystemRichness: " + ecosystem.richness() + "\n"
                + "- ecosystemEvenness: " + fmt(ecosystem.evenness()) + "\n"
                + "- dominanceRatio: " + fmt(ecosystem.dominanceRatio()) + "\n"
                + "- branchEntropy: " + fmt(branch.entropy()) + "\n"
                + "- traitVariance: " + fmt(trait.variance()) + "\n"
                + "- lineageConcentration: " + fmt(lineage.concentration()) + "\n\n"
                + "## Notes\n"
                + "- ecosystemRichness counts represented families.\n"
                + "- ecosystemEvenness is Shannon evenness (Pielou's J).\n"
                + "- dominanceRatio is the top family's share of total abundance.\n"
                + "- branchEntropy is Shannon entropy over branch abundances.\n"
                + "- traitVariance is variance of combined trigger+mechanic abundance values.\n"
                + "- lineageConcentration is HHI over lineage abundance (derived from branch prefixes).\n";
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
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
