package obtuseloot.ecosystem;

import obtuseloot.analytics.EcosystemHealthReport;
import obtuseloot.analytics.EnvironmentalPressureReporter;
import obtuseloot.simulation.worldlab.SimulationMetricsCollector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ArtifactEcosystemSelfBalancingEngine {
    private final EcosystemBiasCalculator biasCalculator = new EcosystemBiasCalculator();
    private final EcosystemBiasState biasState = new EcosystemBiasState();
    private final ArtifactEcosystemBalancer balancer = new ArtifactEcosystemBalancer();
    private final EcosystemDiversityController diversityController = new EcosystemDiversityController();
    private final GeneratorWeightController weightController = new GeneratorWeightController();
    private final EnvironmentPressureEngine pressureEngine = new EnvironmentPressureEngine();
    private final EnvironmentalPressureReporter environmentalPressureReporter = new EnvironmentalPressureReporter();
    private final Map<String, Double> branchShares = new ConcurrentHashMap<>();

    private ProductionSafetyGuards safetyGuards;
    private EcosystemHealthMonitor healthMonitor;

    public ArtifactEcosystemSelfBalancingEngine() {
        Logger defaultLogger = Logger.getLogger(ArtifactEcosystemSelfBalancingEngine.class.getName());
        ProductionSafetyConfig defaultConfig = ProductionSafetyConfig.defaults();
        this.safetyGuards = new ProductionSafetyGuards(defaultConfig, defaultLogger);
        this.healthMonitor = new EcosystemHealthMonitor(safetyGuards, defaultLogger,
                defaultConfig.snapshotIntervalEvents(), defaultConfig.periodicConsoleLog());
    }

    /**
     * Configure production safety guards with server-supplied config and logger.
     * Must be called once during plugin startup before the first {@link #evaluate} call.
     */
    public void configure(ProductionSafetyConfig config, Logger logger) {
        this.safetyGuards = new ProductionSafetyGuards(config, logger);
        this.healthMonitor = new EcosystemHealthMonitor(safetyGuards, logger,
                config.snapshotIntervalEvents(), config.periodicConsoleLog());
    }

    public void evaluate(WorldEcosystemProfile profile, EcosystemHealthReport report, SimulationMetricsCollector metrics) {
        biasState.mergeTarget(biasCalculator.calculate(profile));
        weightController.applyEcosystemBias(biasState);
        weightController.applyBalanceAdjustments(balancer.computeAdjustments(metrics.families()));
        weightController.applyDiversityAdjustments(diversityController.computeAdjustments(metrics.families()));
        pressureEngine.advanceSeason();
        updateBranchShares(metrics.branches());

        // Safety guard observation — feed current distributions into rolling windows
        Map<String, Double> categoryShares = toShares(metrics.families());
        Map<String, Double> templateShares = toShares(metrics.triggers());
        healthMonitor.recordEvaluation(categoryShares, templateShares, -1);

        try {
            environmentalPressureReporter.writeReport(Path.of("analytics/environment-pressure-report.md"), pressureEngine);
        } catch (IOException ignored) {
            // report generation is best-effort for analytics integration.
        }
    }

    /**
     * Returns the final weight for a family, including the production safety category guard multiplier.
     * The guard is bounded, deterministic, and reversible — it returns to 1.0 once the distribution
     * normalises inside the rolling window.
     */
    public double weightForFamily(String family) {
        double base = weightController.finalWeight(family);
        return base * safetyGuards.categoryGuardMultiplier(family);
    }

    public double branchShare(String branchId) {
        if (branchId == null || branchId.isBlank()) {
            return 0.0D;
        }
        return branchShares.getOrDefault(branchId, 0.0D);
    }

    /**
     * Record an observed candidate pool size from the ability selection pipeline.
     * Pool sizes below {@link ProductionSafetyConfig#candidatePoolCollapseThreshold()} trigger a
     * logged warning and enable eligibility relaxation via {@link ProductionSafetyGuards#isPoolCollapsed()}.
     */
    public void recordCandidatePoolSize(int poolSize) {
        safetyGuards.recordPoolSize(poolSize);
    }

    /** Whether the last reported candidate pool was below the collapse threshold. */
    public boolean isCandidatePoolCollapsed() {
        return safetyGuards.isPoolCollapsed();
    }

    /** Capture a point-in-time production safety snapshot (runs failure detection). */
    public ProductionSafetySnapshot captureSnapshot() {
        return healthMonitor.captureSnapshot();
    }

    /** Access the health monitor for command display and metric queries. */
    public EcosystemHealthMonitor healthMonitor() {
        return healthMonitor;
    }

    /** Access the safety guards (for multiplier queries and metrics). */
    public ProductionSafetyGuards safetyGuards() {
        return safetyGuards;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bias", new LinkedHashMap<>(biasState.biasByFamily()));
        out.put("biasWeights", new LinkedHashMap<>(weightController.ecosystemBiasWeights()));
        out.put("balanceWeights", new LinkedHashMap<>(weightController.balanceAdjustmentWeights()));
        out.put("diversityWeights", new LinkedHashMap<>(weightController.diversityAdjustmentWeights()));
        out.put("updates", biasState.updates());
        out.put("environment", pressureEngine.currentEvent().name());
        out.put("environmentRemainingSeasons", pressureEngine.currentEvent().remainingSeasons());
        out.put("environmentModifiers", new LinkedHashMap<>(pressureEngine.currentModifiers()));
        out.put("branchShares", new LinkedHashMap<>(branchShares));
        // Include safety metrics in engine snapshot
        ProductionSafetySnapshot safetySnap = healthMonitor.lastSnapshot();
        Map<String, Object> safety = new LinkedHashMap<>();
        safety.put("diversityIndex", safetySnap.diversityIndex());
        safety.put("avgCandidatePoolSize", safetySnap.averageCandidatePoolSize());
        safety.put("windowFill", safetySnap.windowFill());
        safety.put("activeGuards", safetySnap.activeGuards());
        safety.put("activeFailureSignals", safetySnap.activeFailureSignals());
        out.put("safety", safety);
        return out;
    }

    public EnvironmentPressureEngine pressureEngine() {
        return pressureEngine;
    }

    private void updateBranchShares(Map<String, Integer> branches) {
        int total = branches.values().stream().mapToInt(Integer::intValue).sum();
        branchShares.clear();
        if (total <= 0) {
            return;
        }
        for (Map.Entry<String, Integer> entry : branches.entrySet()) {
            branchShares.put(entry.getKey(), entry.getValue() / (double) total);
        }
    }

    public void persist(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, snapshot().toString());
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static Map<String, Double> toShares(Map<String, Integer> counts) {
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) return Collections.emptyMap();
        Map<String, Double> shares = new LinkedHashMap<>();
        counts.forEach((k, v) -> shares.put(k.toLowerCase(java.util.Locale.ROOT), v / (double) total));
        return shares;
    }
}
