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
    private static final int SURVIVAL_WINDOW = 8;
    private static final double UNSTABLE_THRESHOLD = 0.22D;
    private static final double COLLAPSING_THRESHOLD = 0.05D;
    private static final int UNSTABLE_GRACE_WINDOWS = 3;
    private static final int COLLAPSING_GRACE_WINDOWS = 3;
    private static final int RECOVERY_WINDOWS = 2;

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
    private final Deque<BranchLifecycleTransition> recentBranchTransitions = new ArrayDeque<>();
    private int repeatedDivergences;
    private double currentBranchDivergence;
    private int driftWindowTicks = 5;
    private int descendantsObserved;
    private int branchBirths;
    private int branchCollapses;
    private int branchSurvivors;

    public ArtifactLineage(String lineageId) {
        this.lineageId = lineageId;
        this.evolutionaryBiasGenome = new EvolutionaryBiasGenome();
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
    public double currentBranchDivergence() { return currentBranchDivergence; }
    public int descendantsObserved() { return descendantsObserved; }
    public int branchBirths() { return branchBirths; }
    public int branchCollapses() { return branchCollapses; }
    public int branchSurvivors() { return branchSurvivors; }
    public int driftWindowTicks() { return driftWindowTicks; }
    public List<Double> utilityDensityHistory() { return List.copyOf(utilityDensityHistory); }
    public List<Double> ecologicalPressureHistory() { return List.copyOf(ecologicalPressureHistory); }
    public List<Double> specializationTrajectory() { return List.copyOf(specializationTrajectory); }
    public List<BranchLifecycleTransition> consumeRecentBranchTransitions() {
        List<BranchLifecycleTransition> out = new ArrayList<>(recentBranchTransitions);
        recentBranchTransitions.clear();
        return out;
    }

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
        currentBranchDivergence = divergence;
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

        String formedSignature = null;
        boolean shouldBranch = branchingHeuristics.shouldBranch(evolutionaryBiasGenome, observedBias, repeatedDivergences, ecologicalPressure)
                || (observedDescendantBiases.size() >= 8 && divergence > 0.08D);
        if (shouldBranch) {
            formedSignature = branchSignature(observedBias);
            final String branchSignature = formedSignature;
            LineageBranchProfile branch = branches.computeIfAbsent(branchSignature,
                    ignored -> new LineageBranchProfile(lineageId + ":" + branchSignature, lineageId, observedBias.copy()));
            branch.registerMember(artifactSeed);
            branchBirths++;
            repeatedDivergences = 0;
        }

        String contributingSignature = (formedSignature != null) ? formedSignature : branchSignature(observedBias);
        evaluateBranchLifecycle(contributingSignature);
        branchSurvivors = (int) branches.values().stream().filter(candidate -> candidate.lifecycleState() == BranchLifecycleState.STABLE).count();
    }

    public double specializationTrajectoryDelta() {
        if (specializationTrajectory.size() < 2) {
            return 0.0D;
        }
        return specializationTrajectory.peekLast() - specializationTrajectory.peekFirst();
    }

    public String dominantBranchId() {
        return branches.values().stream()
                .max(java.util.Comparator.comparingInt(LineageBranchProfile::stabilizationCount))
                .map(LineageBranchProfile::branchId)
                .orElse("");
    }

    private void evaluateBranchLifecycle(String formedSignature) {
        List<String> toRemove = new ArrayList<>();
        double recentUtility = bounded(avgTail(utilityDensityHistory, SURVIVAL_WINDOW), 0.0D, 1.0D);
        double ecologicalFit = bounded(1.0D - Math.max(0.0D, avgTail(ecologicalPressureHistory, SURVIVAL_WINDOW) - 1.0D), 0.0D, 1.0D);
        double lineageMomentum = bounded((recentUtility * 0.55D) + (ecologicalFit * 0.45D), 0.0D, 1.0D);
        int branchCount = Math.max(1, branches.size());

        for (Map.Entry<String, LineageBranchProfile> entry : branches.entrySet()) {
            String signature = entry.getKey();
            LineageBranchProfile branch = entry.getValue();
            boolean contributed = signature.equals(formedSignature);
            branch.advanceWindow(contributed);

            double specializationStrength = bounded(
                    Math.abs(branch.biasGenome().tendency(LineageBiasDimension.SPECIALIZATION)) * 0.70D
                            + Math.abs(branchingDistanceFromLineage(branch)) * 1.30D,
                    0.0D,
                    1.0D);
            double contributionStrength = bounded(branch.stabilizationCount() / (double) Math.max(1, descendantsObserved), 0.0D, 1.0D);
            double crowdingPenalty = bounded(Math.max(0.0D, branchCount - 2) * 0.088D * (1.0D - recentUtility)
                    + averageSimilarityPenalty(branch), 0.0D, 0.85D);
            double stagnationPenalty = bounded(branch.windowsSinceLastContribution() * 0.05D, 0.0D, 0.75D);
            double maintenanceCost = bounded(
                    ageCoupledCost(branch.ageWindows(), recentUtility)
                            + (branchCount > 2 ? Math.max(0.0D, branchCount - 2) * 0.045D * (1.0D - contributionStrength) : 0.0D)
                            + crowdingPenalty * 0.38D
                            + Math.max(0.0D, 0.25D - specializationStrength) * 0.30D
                            + Math.max(0.0D, 0.30D - lineageMomentum) * 0.30D,
                    0.0D,
                    1.2D);

            double survivalScore = bounded(
                    (recentUtility * 0.34D)
                            + (ecologicalFit * 0.18D)
                            + (specializationStrength * 0.18D)
                            + (contributionStrength * 0.15D)
                            + (lineageMomentum * 0.15D)
                            - maintenanceCost
                            - stagnationPenalty * 0.45D
                            - crowdingPenalty * 0.35D,
                    -1.2D,
                    1.3D);
            branch.applySurvivalSignals(survivalScore, maintenanceCost, crowdingPenalty, stagnationPenalty);

            BranchLifecycleState before = branch.lifecycleState();
            BranchLifecycleState after = before;
            int weakWindows = branch.persistentWeakWindows();
            int recoverWindows = branch.persistentRecoveryWindows();
            int grace = branch.collapseGraceRemaining();
            boolean collapsed = false;
            String reason = "na";

            if (survivalScore < COLLAPSING_THRESHOLD) {
                weakWindows++;
                recoverWindows = 0;
            } else if (survivalScore >= UNSTABLE_THRESHOLD) {
                recoverWindows++;
                weakWindows = Math.max(0, weakWindows - 1);
            } else {
                weakWindows = Math.max(0, weakWindows - 1);
                recoverWindows = 0;
            }

            if (before == BranchLifecycleState.STABLE && weakWindows >= 2) {
                after = BranchLifecycleState.UNSTABLE;
                grace = UNSTABLE_GRACE_WINDOWS;
                reason = "low-survival-score";
                branch.resetStagnation();
            } else if (before == BranchLifecycleState.UNSTABLE) {
                if (recoverWindows >= RECOVERY_WINDOWS) {
                    after = BranchLifecycleState.STABLE;
                    grace = 0;
                    reason = "recovered";
                } else if (survivalScore < COLLAPSING_THRESHOLD) {
                    grace = Math.max(0, grace - 1);
                    if (grace == 0 && weakWindows >= 3) {
                        after = BranchLifecycleState.COLLAPSING;
                        grace = COLLAPSING_GRACE_WINDOWS;
                        reason = "persistent-weakness";
                    }
                } else {
                    grace = Math.min(UNSTABLE_GRACE_WINDOWS, grace + 1);
                }
            } else if (before == BranchLifecycleState.COLLAPSING) {
                if (recoverWindows >= RECOVERY_WINDOWS + 1) {
                    after = BranchLifecycleState.UNSTABLE;
                    grace = UNSTABLE_GRACE_WINDOWS;
                    reason = "collapse-recovery";
                } else {
                    grace = Math.max(0, grace - 1);
                    if (grace == 0) {
                        collapsed = true;
                        reason = collapseReason(crowdingPenalty, stagnationPenalty, maintenanceCost);
                        toRemove.add(signature);
                    }
                }
            }

            branch.setPersistentWeakWindows(weakWindows);
            branch.setPersistentRecoveryWindows(recoverWindows);
            branch.setCollapseGraceRemaining(grace);
            branch.setLifecycleState(after);
            if (!"na".equals(reason)) {
                branch.setLastCollapseReason(reason);
            }

            if (before != after || collapsed) {
                recentBranchTransitions.addLast(new BranchLifecycleTransition(
                        branch.branchId(),
                        before,
                        after,
                        grace,
                        survivalScore,
                        maintenanceCost,
                        collapsed ? reason : ("na".equals(reason) ? "state-shift" : reason),
                        collapsed));
            }
        }

        for (String signature : toRemove) {
            branches.remove(signature);
            branchCollapses++;
        }

        while (recentBranchTransitions.size() > 32) {
            recentBranchTransitions.removeFirst();
        }
    }

    private double averageSimilarityPenalty(LineageBranchProfile branch) {
        if (branches.size() <= 1) {
            return 0.0D;
        }
        double totalSimilarity = 0.0D;
        int comparisons = 0;
        for (LineageBranchProfile candidate : branches.values()) {
            if (candidate == branch) {
                continue;
            }
            double distance = new InheritanceBranchingHeuristics().distance(branch.biasGenome(), candidate.biasGenome());
            totalSimilarity += bounded(0.20D - distance, 0.0D, 0.20D) * 3.5D;
            comparisons++;
        }
        return comparisons <= 0 ? 0.0D : bounded(totalSimilarity / comparisons, 0.0D, 0.70D);
    }

    private double branchingDistanceFromLineage(LineageBranchProfile branch) {
        return new InheritanceBranchingHeuristics().distance(evolutionaryBiasGenome, branch.biasGenome());
    }

    private double ageCoupledCost(int ageWindows, double recentUtility) {
        double ageFactor = bounded(ageWindows / 18.0D, 0.0D, 1.0D);
        return ageFactor * Math.max(0.0D, 0.58D - recentUtility) * 0.60D;
    }

    private double avgTail(Deque<Double> values, int window) {
        if (values.isEmpty()) {
            return 0.0D;
        }
        int skip = Math.max(0, values.size() - Math.max(1, window));
        int i = 0;
        double sum = 0.0D;
        int count = 0;
        for (Double value : values) {
            if (i++ < skip) {
                continue;
            }
            sum += value;
            count++;
        }
        return count == 0 ? 0.0D : sum / count;
    }

    private String collapseReason(double crowdingPenalty, double stagnationPenalty, double maintenanceCost) {
        if (crowdingPenalty > 0.4D) {
            return "crowding-pressure";
        }
        if (stagnationPenalty > 0.45D) {
            return "stagnation";
        }
        if (maintenanceCost > 0.75D) {
            return "maintenance-overload";
        }
        return "persistent-weakness";
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

    private double bounded(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
