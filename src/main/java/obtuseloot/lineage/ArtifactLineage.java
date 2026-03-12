package obtuseloot.lineage;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ArtifactLineage {
    private static final int OBSERVATION_WINDOW = 16;
    private static final int SPECIALIZATION_WINDOW = 24;

    private final String lineageId;
    private int generationIndex;
    private final List<Long> ancestorSeeds = new ArrayList<>();
    private final List<ArtifactAncestor> ancestors = new ArrayList<>();
    private final Map<String, Double> lineageTraits = new LinkedHashMap<>();
    private final EnumMap<GenomeTrait, Double> genomeTraits = new EnumMap<>(GenomeTrait.class);
    private final EvolutionaryBiasGenome evolutionaryBiasGenome;
    private final Map<String, LineageBranchProfile> branches = new LinkedHashMap<>();
    private final Deque<EvolutionaryBiasGenome> observedDescendantBiases = new ArrayDeque<>();
    private final Deque<Double> utilityDensityHistory = new ArrayDeque<>();
    private final Deque<Double> ecologicalPressureHistory = new ArrayDeque<>();
    private final Deque<Double> specializationTrajectory = new ArrayDeque<>();
    private int repeatedDivergences;
    private int driftWindowTicks = 5;
    private int descendantsObserved;
    private int branchBirths;
    private int branchCollapses;
    private int branchSurvivors;

    public ArtifactLineage(String lineageId) {
        this.lineageId = lineageId;
        this.evolutionaryBiasGenome = EvolutionaryBiasGenome.seeded(lineageId.hashCode());
        for (GenomeTrait trait : GenomeTrait.values()) {
            genomeTraits.put(trait, 0.0D);
        }
        for (String trait : new String[]{"precision", "brutality", "survival", "mobility", "chaos", "consistency", "mutation", "memory"}) {
            lineageTraits.put(trait, 0.0D);
        }
    }

    public String lineageId() { return lineageId; }
    public int depth() { return generationIndex; }
    public int generationIndex() { return generationIndex; }
    public List<Long> ancestorSeeds() { return List.copyOf(ancestorSeeds); }
    public List<ArtifactAncestor> ancestors() { return ancestors; }
    public Map<String, Double> lineageTraits() { return lineageTraits; }
    public Map<GenomeTrait, Double> genomeTraits() { return Map.copyOf(genomeTraits); }
    public EvolutionaryBiasGenome evolutionaryBiasGenome() { return evolutionaryBiasGenome; }
    public Map<String, LineageBranchProfile> branches() { return Map.copyOf(branches); }
    public int repeatedDivergences() { return repeatedDivergences; }
    public int descendantsObserved() { return descendantsObserved; }
    public int branchBirths() { return branchBirths; }
    public int branchCollapses() { return branchCollapses; }
    public int branchSurvivors() { return branchSurvivors; }
    public int driftWindowTicks() { return driftWindowTicks; }
    public List<Double> utilityDensityHistory() { return List.copyOf(utilityDensityHistory); }
    public List<Double> ecologicalPressureHistory() { return List.copyOf(ecologicalPressureHistory); }
    public List<Double> specializationTrajectory() { return List.copyOf(specializationTrajectory); }

    public void addAncestor(ArtifactAncestor ancestor) {
        ancestors.add(ancestor);
        generationIndex = Math.max(generationIndex, ancestor.generationIndex());
        ancestorSeeds.add(ancestor.artifactSeed());
    }

    public void registerGenome(long ancestorSeed, ArtifactGenome genome) {
        int nextGeneration = generationIndex + 1;
        addAncestor(new ArtifactAncestor(ancestorSeed, nextGeneration));
        for (GenomeTrait trait : GenomeTrait.values()) {
            genomeTraits.put(trait, genome.trait(trait));
        }
    }

    public void applyMutation(LineageMutation mutation) {
        double current = lineageTraits.getOrDefault(mutation.trait(), 0.0D);
        lineageTraits.put(mutation.trait(), clampTrait(current + mutation.delta()));
    }

    public void registerDescendantBias(long artifactSeed,
                                       EvolutionaryBiasGenome observedBias,
                                       double ecologicalPressure,
                                       double mutationInfluence,
                                       double driftWindow,
                                       double utilityDensity,
                                       InheritanceBranchingHeuristics branchingHeuristics) {
        descendantsObserved++;
        observedDescendantBiases.addLast(observedBias.copy());
        while (observedDescendantBiases.size() > OBSERVATION_WINDOW) {
            observedDescendantBiases.removeFirst();
        }
        utilityDensityHistory.addLast(utilityDensity);
        ecologicalPressureHistory.addLast(ecologicalPressure);
        specializationTrajectory.addLast(observedBias.tendency(LineageBiasDimension.SPECIALIZATION));
        while (utilityDensityHistory.size() > SPECIALIZATION_WINDOW) {
            utilityDensityHistory.removeFirst();
        }
        while (ecologicalPressureHistory.size() > SPECIALIZATION_WINDOW) {
            ecologicalPressureHistory.removeFirst();
        }
        while (specializationTrajectory.size() > SPECIALIZATION_WINDOW) {
            specializationTrajectory.removeFirst();
        }

        double divergence = branchingHeuristics.distance(evolutionaryBiasGenome, observedBias);
        double adaptationStrength = Math.max(0.05D,
                (0.22D * mutationInfluence)
                        - (Math.max(0.0D, ecologicalPressure - 1.0D) * 0.14D)
                        - (driftWindowTicks <= 0 ? Math.max(0.0D, ecologicalPressure - 1.0D) * 0.05D : 0.0D));
        evolutionaryBiasGenome.mergeToward(observedBias, adaptationStrength);

        Random driftRandom = new Random(artifactSeed ^ lineageId.hashCode());
        double pressureAcceleration = Math.max(0.0D, ecologicalPressure - 1.0D) * 0.030D;
        double effectiveDrift = driftWindowTicks > 0
                ? driftWindow * (0.35D + pressureAcceleration)
                : driftWindow * (1.0D + pressureAcceleration + Math.max(0.0D, divergence - 0.08D));
        evolutionaryBiasGenome.applyDrift(driftRandom, Math.max(0.003D, effectiveDrift));
        if (driftWindowTicks > 0) {
            driftWindowTicks--;
        }

        if (divergence > 0.12D) {
            repeatedDivergences++;
        } else {
            repeatedDivergences = Math.max(0, repeatedDivergences - 1);
        }
        boolean shouldBranch = branchingHeuristics.shouldBranch(evolutionaryBiasGenome, observedBias, repeatedDivergences, ecologicalPressure)
                || (observedDescendantBiases.size() >= 6 && divergence > 0.05D);
        if (shouldBranch) {
            String signature = branchSignature(observedBias);
            LineageBranchProfile branch = branches.computeIfAbsent(signature,
                    ignored -> new LineageBranchProfile(lineageId + ":" + signature, lineageId, observedBias.copy()));
            branch.registerMember(artifactSeed);
            branchBirths++;
            branchSurvivors = (int) branches.values().stream().filter(candidate -> candidate.stabilizationCount() >= 2).count();
            repeatedDivergences = 0;
        }
        int collapses = 0;
        for (LineageBranchProfile branch : branches.values()) {
            if (branch.stabilizationCount() == 1 && ecologicalPressure > 1.25D) {
                collapses++;
            }
        }
        branchCollapses = collapses;
    }

    public String dominantBranchId() {
        return branches.values().stream()
                .max(java.util.Comparator.comparingInt(LineageBranchProfile::stabilizationCount))
                .map(LineageBranchProfile::branchId)
                .orElse("");
    }

    private String branchSignature(EvolutionaryBiasGenome biasGenome) {
        if (biasGenome.tendency(LineageBiasDimension.SUPPORT_PREFERENCE) > 0.12D) {
            return "support";
        }
        if (biasGenome.tendency(LineageBiasDimension.EXPLORATION_PREFERENCE) > 0.12D) {
            return "explorer";
        }
        if (biasGenome.tendency(LineageBiasDimension.RITUAL_PREFERENCE) > 0.12D) {
            return "ritual";
        }
        if (biasGenome.tendency(LineageBiasDimension.MEMORY_REACTIVITY) > 0.12D) {
            return "witness";
        }
        return "adaptive";
    }

    private double clampTrait(double value) {
        return Math.max(-0.10D, Math.min(0.10D, value));
    }
}
