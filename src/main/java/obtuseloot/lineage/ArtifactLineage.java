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

    private final String lineageId;
    private int generationIndex;
    private final List<Long> ancestorSeeds = new ArrayList<>();
    private final List<ArtifactAncestor> ancestors = new ArrayList<>();
    private final Map<String, Double> lineageTraits = new LinkedHashMap<>();
    private final EnumMap<GenomeTrait, Double> genomeTraits = new EnumMap<>(GenomeTrait.class);
    private final EvolutionaryBiasGenome evolutionaryBiasGenome;
    private final Map<String, LineageBranchProfile> branches = new LinkedHashMap<>();
    private final Deque<EvolutionaryBiasGenome> observedDescendantBiases = new ArrayDeque<>();
    private int repeatedDivergences;

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
                                       InheritanceBranchingHeuristics branchingHeuristics) {
        observedDescendantBiases.addLast(observedBias.copy());
        while (observedDescendantBiases.size() > OBSERVATION_WINDOW) {
            observedDescendantBiases.removeFirst();
        }

        double divergence = branchingHeuristics.distance(evolutionaryBiasGenome, observedBias);
        double adaptationStrength = Math.max(0.06D, 0.22D - (Math.max(0.0D, ecologicalPressure - 1.0D) * 0.08D));
        evolutionaryBiasGenome.mergeToward(observedBias, adaptationStrength);

        Random driftRandom = new Random(artifactSeed ^ lineageId.hashCode());
        evolutionaryBiasGenome.applyDrift(driftRandom, 0.035D);

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
            repeatedDivergences = 0;
        }
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
