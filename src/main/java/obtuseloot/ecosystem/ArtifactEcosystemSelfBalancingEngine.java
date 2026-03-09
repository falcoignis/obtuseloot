package obtuseloot.ecosystem;

import obtuseloot.analytics.EcosystemHealthReport;
import obtuseloot.analytics.EnvironmentalPressureReporter;
import obtuseloot.simulation.worldlab.SimulationMetricsCollector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ArtifactEcosystemSelfBalancingEngine {
    private final EcosystemBiasCalculator biasCalculator = new EcosystemBiasCalculator();
    private final EcosystemBiasState biasState = new EcosystemBiasState();
    private final ArtifactEcosystemBalancer balancer = new ArtifactEcosystemBalancer();
    private final EcosystemDiversityController diversityController = new EcosystemDiversityController();
    private final GeneratorWeightController weightController = new GeneratorWeightController();
    private final EnvironmentPressureEngine pressureEngine = new EnvironmentPressureEngine();
    private final EnvironmentalPressureReporter environmentalPressureReporter = new EnvironmentalPressureReporter();

    public void evaluate(WorldEcosystemProfile profile, EcosystemHealthReport report, SimulationMetricsCollector metrics) {
        biasState.mergeTarget(biasCalculator.calculate(profile));
        weightController.applyEcosystemBias(biasState);
        weightController.applyBalanceAdjustments(balancer.computeAdjustments(metrics.families()));
        weightController.applyDiversityAdjustments(diversityController.computeAdjustments(metrics.families()));
        pressureEngine.advanceSeason();
        try {
            environmentalPressureReporter.writeReport(Path.of("analytics/environment-pressure-report.md"), pressureEngine);
        } catch (IOException ignored) {
            // report generation is best-effort for analytics integration.
        }
    }

    public double weightForFamily(String family) {
        return weightController.finalWeight(family);
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
        return out;
    }

    public EnvironmentPressureEngine pressureEngine() {
        return pressureEngine;
    }

    public void persist(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, snapshot().toString());
    }
}
