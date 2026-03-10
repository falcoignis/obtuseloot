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
        double lineageDiscipline = lineage == null ? 0.0D : Math.max(0.0D, lineage.lineageTraits().getOrDefault("discipline", 0.0D));
        double lineageSurvival = lineage == null ? 0.0D : Math.max(0.0D, lineage.lineageTraits().getOrDefault("survival", 0.0D));
        double lineageChaos = lineage == null ? 0.0D : Math.max(0.0D, lineage.lineageTraits().getOrDefault("chaos", 0.0D));
        double specialization = specializationIndex(resonance, volatility, stability, mobility, survival, precision);
        double breadthPenalty = 1.0D - (specialization * 0.18D);

        gate(gates, RegulatoryGate.RESONANCE, (resonance + lineageResonance * 0.62D + specialization * 0.18D) / 1.32D, 0.61D);
        gate(gates, RegulatoryGate.VOLATILITY,
                ((volatility + genome.trait(GenomeTrait.CHAOS_AFFINITY) * 0.85D + lineageChaos * 0.22D) / 2.07D) * breadthPenalty,
                0.55D);
        gate(gates, RegulatoryGate.MOBILITY, ((mobility + memory.mobilityWeight() * 0.35D) / 1.5D) * breadthPenalty, 0.53D);
        gate(gates, RegulatoryGate.SURVIVAL,
                ((survival + memory.survivalWeight() * 0.4D + memory.traumaWeight() * 0.25D + lineageSurvival * 0.2D) / 1.85D) * breadthPenalty,
                0.56D);
        gate(gates, RegulatoryGate.DISCIPLINE,
                (precision + stability + memory.disciplineWeight() * 0.35D + lineageDiscipline * 0.3D + specialization * 0.15D) / 2.8D,
                0.59D);
        gate(gates, RegulatoryGate.MEMORY,
                (memory.pressure() * 0.06D + survival + memory.disciplineWeight() * 0.25D + lineageDepth * 0.22D) / 2.1D,
                0.58D);

        double envMultiplier = pressureEngine == null ? 1.0D : pressureEngine.multiplierFor(GenomeTrait.PRECISION_AFFINITY);
        double envSignal = pressureEngine == null ? 0.0D : environmentSignal(pressureEngine, artifact);
        gate(gates, RegulatoryGate.ENVIRONMENT,
                ((envSignal * 1.15D) + envMultiplier + genome.trait(GenomeTrait.MUTATION_SENSITIVITY) + lineageDepth * 0.2D) / 3.35D,
                0.57D);

        gate(gates, RegulatoryGate.LINEAGE_MILESTONE,
                (lineageDepth + (artifact.getTotalDrifts() / 14.0D) + (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath()) ? 0.30D : 0.0D)),
                0.74D);

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

    private double specializationIndex(double... values) {
        if (values.length == 0) {
            return 0.0D;
        }
        double max = 0.0D;
        double sum = 0.0D;
        for (double value : values) {
            max = Math.max(max, value);
            sum += value;
        }
        double avg = sum / values.length;
        return Math.max(0.0D, Math.min(1.0D, max - avg));
    }
}
