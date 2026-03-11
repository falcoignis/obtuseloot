package obtuseloot.dashboard;

import obtuseloot.analytics.EcosystemStatus;
import obtuseloot.analytics.EcologyDiagnosticState;
import obtuseloot.analytics.RankAbundanceAnalyzer;
import obtuseloot.analytics.RankAbundanceAnalyzer.RankAbundanceResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DashboardService {
    private static final Pattern SECTION_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*\\{([^}]*)}", Pattern.DOTALL);
    private static final Pattern ENTRY_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    private final RankAbundanceAnalyzer analyzer = new RankAbundanceAnalyzer();
    private final EcosystemDashboard ecosystemDashboard = new EcosystemDashboard();
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

        EcosystemGaugeData gaugeData = loadEcosystemGaugeData();
        EcologicalMemoryData ecologicalMemory = loadEcologicalMemoryData();

        return new DashboardMetrics(
                ecosystem.dominanceRatio(),
                branch.entropy(),
                lineage.concentration(),
                trait.variance(),
                collapseRisk,
                gaugeData.endArtifacts(),
                gaugeData.endSpecies(),
                gaugeData.latestTnt(),
                gaugeData.latestNser(),
                gaugeData.endTrend(),
                gaugeData.tntTrend(),
                gaugeData.nserTrend(),
                gaugeData.nserInterpretation(),
                gaugeData.status(),
                gaugeData.diagnosticState(),
                gaugeData.diagnosticConfidence(),
                gaugeData.warningFlags(),
                ecologicalMemory.active(),
                ecologicalMemory.attractorDuration(),
                ecologicalMemory.memoryPressure());
    }

    public Path regenerateDashboard() throws IOException {
        return ecosystemDashboard.generate(analyticsRoot, calculateMetrics(), dashboardFile());
    }

    public Path generateSeasonDashboard(int season) throws IOException {
        Path output = analyticsRoot.resolve("world-lab").resolve("season" + season + "-ecosystem-dashboard.html");
        return ecosystemDashboard.generate(analyticsRoot, calculateMetrics(), output);
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

    private EcosystemGaugeData loadEcosystemGaugeData() throws IOException {
        Path path = analyticsRoot.resolve("ecosystem-health-gauge.json");
        if (!Files.exists(path)) {
            return new EcosystemGaugeData(0.0D, null, 0.0D, 0.0D, List.of(), List.of(), List.of(), "NSER not available.", EcosystemStatus.STAGNANT, EcologyDiagnosticState.STAGNANT_ATTRACTOR, 0.0D, List.of("stagnation"));
        }
        String content = Files.readString(path);
        double endArtifacts = extractNumber(content, "END_artifacts");
        Double endSpecies = extractNullableNumber(content, "END_species");
        List<Double> endTrend = extractDoubleArray(content, "END_trend");
        List<Double> tntTrend = extractDoubleArray(content, "TNT_trend");
        List<Double> nserTrend = extractDoubleArray(content, "NSER_trend");
        double latestNser = nserTrend.isEmpty() ? extractNumber(content, "NSER_latest") : nserTrend.get(nserTrend.size() - 1);
        String interpretation = extractString(content, "interpretation");
        String statusRaw = extractString(content, "ecosystem_status");
        EcosystemStatus status;
        try {
            status = statusRaw == null || statusRaw.isBlank() ? EcosystemStatus.STAGNANT : EcosystemStatus.valueOf(statusRaw);
        } catch (IllegalArgumentException ex) {
            status = EcosystemStatus.STAGNANT;
        }
        double latestTnt = tntTrend.isEmpty() ? 0.0D : tntTrend.get(tntTrend.size() - 1);
        EcologyDiagnosticState diagnosticState = loadDiagnosticState();
        double confidence = loadDiagnosticConfidence();
        List<String> warnings = loadDiagnosticWarnings();
        return new EcosystemGaugeData(endArtifacts, endSpecies, latestTnt, latestNser, endTrend, tntTrend, nserTrend,
                interpretation == null ? "NSER not available." : interpretation, status, diagnosticState, confidence, warnings);
    }


    private EcologicalMemoryData loadEcologicalMemoryData() throws IOException {
        Path path = analyticsRoot.resolve("world-lab").resolve("world-sim-data.json");
        if (!Files.exists(path)) {
            return new EcologicalMemoryData(false, 0.0D, 0.0D);
        }
        String content = Files.readString(path);
        Matcher section = Pattern.compile("\"ecological_memory\"\\s*:\\s*\\{([^}]*)}", Pattern.DOTALL).matcher(content);
        if (!section.find()) {
            return new EcologicalMemoryData(false, 0.0D, 0.0D);
        }
        String block = section.group(1);
        boolean active = extractBoolean(block, "active");
        double attractorDuration = extractNumber(block, "attractorDuration");
        double pressure = extractNumber(block, "memoryPressure");
        return new EcologicalMemoryData(active, attractorDuration, pressure);
    }

    private EcologyDiagnosticState loadDiagnosticState() throws IOException {
        Path path = analyticsRoot.resolve("ecology-diagnostic-state.json");
        if (!Files.exists(path)) {
            return EcologyDiagnosticState.STAGNANT_ATTRACTOR;
        }
        String raw = extractString(Files.readString(path), "diagnostic_state");
        try {
            return raw == null ? EcologyDiagnosticState.STAGNANT_ATTRACTOR : EcologyDiagnosticState.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return EcologyDiagnosticState.STAGNANT_ATTRACTOR;
        }
    }

    private double loadDiagnosticConfidence() throws IOException {
        Path path = analyticsRoot.resolve("ecology-diagnostic-state.json");
        if (!Files.exists(path)) {
            return 0.0D;
        }
        return extractNumber(Files.readString(path), "confidence");
    }

    private List<String> loadDiagnosticWarnings() throws IOException {
        Path path = analyticsRoot.resolve("ecology-diagnostic-state.json");
        if (!Files.exists(path)) {
            return List.of();
        }
        return extractStringArray(Files.readString(path), "warning_flags");
    }

    private double extractNumber(String content, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(content);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0.0D;
    }

    private Double extractNullableNumber(String content, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(null|[0-9]+(?:\\.[0-9]+)?)").matcher(content);
        if (!matcher.find()) {
            return null;
        }
        return "null".equals(matcher.group(1)) ? null : Double.parseDouble(matcher.group(1));
    }

    private boolean extractBoolean(String content, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)").matcher(content);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private String extractString(String content, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<Double> extractDoubleArray(String content, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^]]*)]", Pattern.DOTALL).matcher(content);
        if (!matcher.find()) {
            return List.of();
        }
        String body = matcher.group(1).trim();
        if (body.isEmpty()) {
            return List.of();
        }
        String[] parts = body.split(",");
        List<Double> out = new java.util.ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(Double.parseDouble(trimmed));
            }
        }
        return out;
    }

    private List<String> extractStringArray(String content, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[([^]]*)]", Pattern.DOTALL).matcher(content);
        if (!matcher.find()) {
            return List.of();
        }
        Matcher strMatcher = Pattern.compile("\"([^\"]+)\"").matcher(matcher.group(1));
        List<String> out = new java.util.ArrayList<>();
        while (strMatcher.find()) {
            out.add(strMatcher.group(1));
        }
        return out;
    }

    private record EcologicalMemoryData(boolean active, double attractorDuration, double memoryPressure) {}

    private record EcosystemGaugeData(double endArtifacts,
                                      Double endSpecies,
                                      double latestTnt,
                                      double latestNser,
                                      List<Double> endTrend,
                                      List<Double> tntTrend,
                                      List<Double> nserTrend,
                                      String nserInterpretation,
                                      EcosystemStatus status,
                                      EcologyDiagnosticState diagnosticState,
                                      double diagnosticConfidence,
                                      List<String> warningFlags) {}
}
