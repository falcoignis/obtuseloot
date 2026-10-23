package obtuseloot.ecology;

import obtuseloot.abilities.genome.GenomeTrait;
import obtuseloot.analytics.NicheDiversityMetrics;
import obtuseloot.artifacts.Artifact;
import java.util.*;
import java.util.function.Function;

/** Measured local diagnostics, not a verdict about ecosystem health or a control on reproduction. */
public record LocalEcosystemHealth(long gameTick, int radiusChunks, long windowTicks,
        int observedItems, int oldRevisions, int eligibleParents, int nonFiniteFitnessItems,
        int observedLineages, int unassignedLineageItems, double lineageSimpson,
        double effectiveParents, double maximumParentShare, boolean finiteSamplerTotal,
        Map<GenomeTrait, TraitSpread> traits, Map<String, Activity> activities) {
    public LocalEcosystemHealth {
        var copy = new EnumMap<GenomeTrait, TraitSpread>(GenomeTrait.class); copy.putAll(traits);
        traits = Collections.unmodifiableMap(copy);
        activities = Collections.unmodifiableMap(new TreeMap<>(activities));
    }

    public record TraitSpread(double mean, double variance, double latentMean, double latentVariance) { }
    public record Activity(long attempts, long meaningfulOutcomes, Double meanUtility,
                           long performanceSamples, Double meanBenefit, Double meanRecoveryTicks) { }

    static LocalEcosystemHealth capture(long tick, int radius, long window,
            List<LocalInheritancePool.LocalEvidence> evidence, Function<String, Artifact> resolver) {
        var lineages = new TreeMap<String, Integer>();
        var genes = new EnumMap<GenomeTrait, Moments>(GenomeTrait.class);
        var latent = new EnumMap<GenomeTrait, Moments>(GenomeTrait.class);
        var activities = new TreeMap<String, ActivityAccumulator>();
        var fitness = new ArrayList<Double>();
        var resolved = new HashMap<String, Artifact>();
        int current = 0, old = 0, nonFinite = 0, unassigned = 0;
        for (var local : evidence) {
            var individual = local.individual();
            var artifact = resolved.computeIfAbsent(individual.storageKey(), resolver);
            if (artifact == null) throw new IllegalStateException("Missing observed item profile " + individual.storageKey());
            if (artifact.getArtifactSeed() != individual.seed() || artifact.getGenomeRevision() != individual.genomeRevision()) {
                old++; continue;
            }
            current++;
            if (!Double.isFinite(individual.fitness())) nonFinite++;
            else if (individual.fitness() > 0) fitness.add(individual.fitness());
            String lineage = artifact.getLatentLineage();
            if (lineage == null || lineage.isBlank() || "unassigned".equalsIgnoreCase(lineage)) unassigned++;
            else lineages.merge(lineage, 1, Integer::sum);
            var genome = artifact.getGenome();
            for (var trait : GenomeTrait.values()) {
                genes.computeIfAbsent(trait, ignored -> new Moments()).add(genome.trait(trait));
                latent.computeIfAbsent(trait, ignored -> new Moments()).add(genome.latentTrait(trait));
            }
            local.activities().forEach((key, signal) -> activities.computeIfAbsent(key, ignored -> new ActivityAccumulator())
                    .add(signal.attempts(), signal.meaningfulOutcomes(), signal.validatedUtility(), local.performance().get(key)));
        }
        var traits = new EnumMap<GenomeTrait, TraitSpread>(GenomeTrait.class);
        genes.forEach((trait, values) -> traits.put(trait,
                new TraitSpread(values.mean, values.variance(), latent.get(trait).mean, latent.get(trait).variance())));
        var measured = new TreeMap<String, Activity>(); activities.forEach((key, value) -> measured.put(key, value.snapshot()));
        // Scaling only computes the existing sampler's probabilities; it never changes its weights.
        double maximum = fitness.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double scaledTotal = fitness.stream().mapToDouble(value -> value / maximum).sum();
        double squares = 0, largestShare = 0;
        for (double value : fitness) {
            double share = (value / maximum) / scaledTotal;
            squares += share * share; largestShare = Math.max(largestShare, share);
        }
        var diversity = NicheDiversityMetrics.diversity(lineages);
        return new LocalEcosystemHealth(tick, radius, window, current, old, fitness.size(), nonFinite,
                diversity.liveNiches(), unassigned, diversity.simpsonDiversity(),
                squares == 0 ? 0 : 1 / squares, largestShare,
                Double.isFinite(fitness.stream().mapToDouble(Double::doubleValue).sum()), traits, measured);
    }

    private static final class Moments {
        long count; double mean, sumSquares;
        void add(double value) { count++; double delta = value - mean; mean += delta / count; sumSquares += delta * (value - mean); }
        double variance() { return Math.max(0, sumSquares / count); }
    }
    private static final class ActivityAccumulator {
        long attempts, meaningful, samples; double utility, benefit, recovery;
        void add(long count, long successes, double value, LocalInheritancePool.PerformanceSummary performance) {
            attempts += count; meaningful += successes; utility += value;
            if (performance != null) {
                long total = Math.addExact(samples, performance.samples());
                double fraction = (double) performance.samples() / total;
                benefit += (performance.meanBenefit() - benefit) * fraction;
                recovery += (performance.meanRecoveryTicks() - recovery) * fraction;
                samples = total;
            }
        }
        Activity snapshot() {
            return new Activity(attempts, meaningful, Double.isFinite(utility) ? utility / attempts : null,
                    samples, samples == 0 ? null : benefit, samples == 0 ? null : recovery);
        }
    }
}
