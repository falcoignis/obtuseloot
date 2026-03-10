package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;
import obtuseloot.artifacts.Artifact;
import obtuseloot.ecosystem.EnvironmentPressureEngine;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.memory.ArtifactMemoryProfile;

import java.util.EnumMap;

public final class RegulatoryGateResolver {
    public AbilityRegulatoryProfile resolve(Artifact artifact,
                                            ArtifactGenome genome,
                                            ArtifactMemoryProfile memory,
                                            ArtifactLineage lineage,
                                            EnvironmentPressureEngine pressureEngine) {
        EnumMap<RegulatoryGate, RegulatoryGateState> gates = new EnumMap<>(RegulatoryGate.class);

        double resonance = genome.trait(GenomeTrait.RESONANCE);
        double volatility = genome.trait(GenomeTrait.VOLATILITY);
        double stability = genome.trait(GenomeTrait.STABILITY);
        double mobility = genome.trait(GenomeTrait.MOBILITY_AFFINITY);
        double survival = genome.trait(GenomeTrait.SURVIVAL_INSTINCT);
        double precision = genome.trait(GenomeTrait.PRECISION_AFFINITY);

        double lineageDepth = lineage == null ? 0.0D : Math.min(1.0D, lineage.generationIndex() / 8.0D);
        double lineageResonance = lineage == null ? 0.0D : Math.max(0.0D, lineage.lineageTraits().getOrDefault("consistency", 0.0D));

        gate(gates, RegulatoryGate.RESONANCE, resonance + lineageResonance * 0.5D, 0.58D);
        gate(gates, RegulatoryGate.VOLATILITY, (volatility + genome.trait(GenomeTrait.CHAOS_AFFINITY)) * 0.5D, 0.52D);
        gate(gates, RegulatoryGate.MOBILITY, (mobility + memory.mobilityWeight() * 0.4D) / 1.4D, 0.50D);
        gate(gates, RegulatoryGate.SURVIVAL, (survival + memory.survivalWeight() * 0.45D + memory.traumaWeight() * 0.30D) / 1.75D, 0.53D);
        gate(gates, RegulatoryGate.DISCIPLINE, (precision + stability + memory.disciplineWeight() * 0.45D) / 2.45D, 0.56D);
        gate(gates, RegulatoryGate.MEMORY, (memory.pressure() * 0.07D + survival + memory.disciplineWeight() * 0.35D) / 2.05D, 0.56D);

        double envMultiplier = pressureEngine == null ? 1.0D : pressureEngine.multiplierFor(GenomeTrait.PRECISION_AFFINITY);
        double envSignal = pressureEngine == null ? 0.0D : environmentSignal(pressureEngine, artifact);
        gate(gates, RegulatoryGate.ENVIRONMENT, (envSignal + envMultiplier + genome.trait(GenomeTrait.MUTATION_SENSITIVITY)) / 3.0D, 0.55D);

        gate(gates, RegulatoryGate.LINEAGE_MILESTONE,
                (lineageDepth + (artifact.getTotalDrifts() / 12.0D) + (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath()) ? 0.35D : 0.0D)),
                0.72D);

        return new AbilityRegulatoryProfile(gates);
    }

    private double environmentSignal(EnvironmentPressureEngine pressureEngine, Artifact artifact) {
        String event = pressureEngine.currentEvent().name().toLowerCase();
        double base = switch (event) {
            case "precisionage" -> artifact.getSeedPrecisionAffinity() + artifact.getSeedConsistencyAffinity();
            case "driftstorm" -> artifact.getSeedChaosAffinity() + artifact.getDriftBias("chaos");
            case "survivalwinter" -> artifact.getSeedSurvivalAffinity() + artifact.getDriftBias("survival");
            case "mobilitybloom" -> artifact.getSeedMobilityAffinity() + artifact.getDriftBias("mobility");
            default -> 0.8D;
        };
        return Math.max(0.0D, Math.min(1.2D, base * 0.6D));
    }

    private void gate(EnumMap<RegulatoryGate, RegulatoryGateState> gates, RegulatoryGate gate, double signal, double threshold) {
        double strength = Math.max(0.0D, Math.min(1.0D, signal));
        gates.put(gate, signal >= threshold ? RegulatoryGateState.open(strength) : RegulatoryGateState.closed(strength));
    }
}
