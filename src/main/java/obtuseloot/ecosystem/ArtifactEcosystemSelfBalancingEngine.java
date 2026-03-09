package obtuseloot.ecosystem;

import obtuseloot.analytics.EcosystemHealthReport;
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
    private final GeneratorWeightController weightController = new GeneratorWeightController();

    public void evaluate(WorldEcosystemProfile profile, EcosystemHealthReport report, SimulationMetricsCollector metrics) {
        biasState.mergeTarget(biasCalculator.calculate(profile));
        weightController.applyEcosystemBias(biasState);
        weightController.applyBalanceAdjustments(balancer.computeAdjustments(metrics.families()));
    }

    public double weightForFamily(String family) {
        return weightController.finalWeight(family);
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bias", new LinkedHashMap<>(biasState.biasByFamily()));
        out.put("biasWeights", new LinkedHashMap<>(weightController.ecosystemBiasWeights()));
        out.put("balanceWeights", new LinkedHashMap<>(weightController.balanceAdjustmentWeights()));
        out.put("updates", biasState.updates());
        return out;
    }

    public void persist(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, snapshot().toString());
    }
}
