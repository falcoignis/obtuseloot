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
        data.put("speciesNicheMetrics", parseSpeciesNicheMetrics(analyticsRoot));
        data.put("coEvolutionMetrics", parseCoEvolutionMetrics(analyticsRoot));
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

    private Map<String, Object> parseSpeciesNicheMetrics(Path analyticsRoot) throws IOException {
        Map<String, Object> out = new LinkedHashMap<>();
        Path speciation = analyticsRoot.resolve("speciation-distribution.json");
        Path nicheMap = analyticsRoot.resolve("species-niche-map.json");
        Path crowding = analyticsRoot.resolve("niche-crowding-distribution.json");
        if (!Files.exists(speciation) || !Files.exists(nicheMap) || !Files.exists(crowding)) {
            return out;
        }
        String speciationContent = Files.readString(speciation);
        String nicheContent = Files.readString(nicheMap);
        String crowdingContent = Files.readString(crowding);
        out.put("activeSpecies", extractNumber(speciationContent, "activeSpecies"));
        out.put("nicheCount", parseIntegerMap(nicheContent, "nicheOccupancy").size());
        out.put("speciesPerNiche", parseIntegerMap(nicheContent, "competingSpeciesPerNiche"));
        out.put("nicheTurnover", parseIntegerMap(nicheContent, "speciesMigrationCounts").values().stream().mapToInt(Integer::intValue).sum());
        out.put("dominantNicheShare", extractMaxShare(crowdingContent, "occupancyByNiche"));
        out.put("overcrowdedNicheCount", extractNumber(crowdingContent, "overcrowdedNicheCount"));
        out.put("dominantSpeciesShare", extractNumber(speciationContent, "dominantSpeciesConcentration"));
        Path fitnessSharing = analyticsRoot.resolve("fitness-sharing-distribution.json");
        if (Files.exists(fitnessSharing)) {
            String sharingContent = Files.readString(fitnessSharing);
            out.put("fitnessSharingActive", extractBoolean(sharingContent, "enabled"));
            out.put("fitnessSharingMode", extractString(sharingContent, "model"));
            out.put("averageSharingLoad", extractNumber(sharingContent, "averageSharingLoad"));
        }
        Path adaptiveCapacity = analyticsRoot.resolve("adaptive-niche-capacity-distribution.json");
        if (Files.exists(adaptiveCapacity)) {
            String content = Files.readString(adaptiveCapacity);
            out.put("adaptiveNicheCapacityEnabled", extractBoolean(content, "enabled"));
            out.put("adaptiveNicheCapacityAverage", averageMapValues(parseDoubleMap(content, "nicheCapacity")));
        }
        Path nicheQuality = analyticsRoot.resolve("niche-quality-diagnostics.json");
        if (Files.exists(nicheQuality)) {
            String content = Files.readString(nicheQuality);
            out.put("roleBasedRepulsionEnabled", extractBoolean(content, "roleBasedRepulsionEnabled"));
            out.put("roleRepulsionBeta", extractNumber(content, "roleRepulsionBeta"));
            out.put("nicheSeparationMode", extractString(content, "roleRepulsionDominance"));
        }
        return out;
    }

    private Map<String, Object> parseCoEvolutionMetrics(Path analyticsRoot) throws IOException {
        Map<String, Object> out = new LinkedHashMap<>();
        Path coEvolution = analyticsRoot.resolve("co-evolution-relationships.json");
        if (!Files.exists(coEvolution)) {
            return out;
        }
        String content = Files.readString(coEvolution);
        out.put("averageCompetitionPressure", extractNumber(content, "averageCompetitionPressure"));
        out.put("averageSupportPressure", extractNumber(content, "averageSupportPressure"));
        out.put("nicheMigrationPressure", extractNumber(content, "nicheMigrationPressure"));
        out.put("dominantAttractorConcentration", extractNumber(content, "dominantAttractorConcentration"));
        out.put("speciesDiversity", extractNumber(content, "speciesDiversity"));
        out.put("coOccurrenceNetworkSize", extractNumber(content, "coOccurrenceNetworkSize"));
        out.put("competitivePairings", parsePairLabels(content, "competitiveRelationships"));
        out.put("supportivePairings", parsePairLabels(content, "supportiveRelationships"));
        return out;
    }

    private java.util.List<String> parsePairLabels(String content, String section) {
        Pattern sectionPattern = Pattern.compile("\"" + Pattern.quote(section) + "\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
        Matcher sectionMatcher = sectionPattern.matcher(content);
        if (!sectionMatcher.find()) {
            return java.util.List.of();
        }
        Matcher pairMatcher = Pattern.compile("\"pair\"\\s*:\\s*\"([^\"]+)\"").matcher(sectionMatcher.group(1));
        java.util.List<String> pairs = new java.util.ArrayList<>();
        while (pairMatcher.find() && pairs.size() < 5) {
            pairs.add(pairMatcher.group(1));
        }
        return pairs;
    }

    private double averageMapValues(Map<String, Double> map) {
        if (map == null || map.isEmpty()) {
            return 0.0D;
        }
        return map.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
    }

    private String extractString(String content, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\s*:\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }

    private boolean extractBoolean(String content, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\s*:\s*(true|false)");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private double extractNumber(String content, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return 0.0D;
        }
        return Double.parseDouble(matcher.group(1));
    }

    private double extractMaxShare(String content, String section) {
        Map<String, Double> map = parseDoubleMap(content, section);
        if (map.isEmpty()) {
            return 0.0D;
        }
        return map.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0D);
    }

    private Map<String, Double> parseDoubleMap(String content, String section) {
        Pattern sectionPattern = Pattern.compile("\"" + Pattern.quote(section) + "\"\\s*:\\s*\\{([^}]*)}", Pattern.DOTALL);
        Matcher sectionMatcher = sectionPattern.matcher(content);
        if (!sectionMatcher.find()) {
            return new LinkedHashMap<>();
        }
        Matcher entryMatcher = ENTRY_PATTERN.matcher(sectionMatcher.group(1));
        Map<String, Double> result = new LinkedHashMap<>();
        while (entryMatcher.find()) {
            result.put(entryMatcher.group(1), Double.parseDouble(entryMatcher.group(2)));
        }
        return result;
    }
}
