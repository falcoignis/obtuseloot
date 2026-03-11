package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class TraitInterferenceResolver {
    private static final int DEFAULT_CACHE_SIZE = 25_000;

    private final Map<String, EnumMap<GenomeTrait, Double>> abilityWeights = new HashMap<>();
    private final TraitProjectionMatrix projectionMatrix = new TraitProjectionMatrix();
    private final Map<String, Double> triggerEfficiencyByAbility = new HashMap<>();
    private final InteractionProjectionMatrix interactionMatrix = new InteractionProjectionMatrix();
    private final ProjectionCache projectionCache;
    private final TraitInterferenceFieldMatrix fieldMatrix = new TraitInterferenceFieldMatrix();
    private final AtomicLong scoringCalls = new AtomicLong();
    private final AtomicLong totalScoringNanos = new AtomicLong();
    private volatile ScoringMode scoringMode = ScoringMode.PROJECTION_WITH_CACHE;

    public TraitInterferenceResolver(List<AbilityTemplate> templates) {
        this(templates, DEFAULT_CACHE_SIZE);
    }

    TraitInterferenceResolver(List<AbilityTemplate> templates, int cacheSize) {
        this.projectionCache = new ProjectionCache(cacheSize);
        registerDefaults(templates);
    }

    public List<AbilityTemplate> selectTop(List<AbilityTemplate> templates, ArtifactGenome genome, int picks) {
        return selectTop(templates, genome, picks, scoringMode);
    }

    public List<AbilityTemplate> selectTop(List<AbilityTemplate> templates, ArtifactGenome genome, int picks, ScoringMode mode) {
        long start = System.nanoTime();
        Map<String, Double> scoreByAbility = scoresByMode(genome, mode);
        scoringCalls.incrementAndGet();
        totalScoringNanos.addAndGet(System.nanoTime() - start);

        List<ScoredTemplate> scored = new ArrayList<>(templates.size());
        for (AbilityTemplate template : templates) {
            double score = scoreByAbility.getOrDefault(template.id(), 0.0D) * triggerEfficiencyByAbility.getOrDefault(template.id(), 1.0D);
            scored.add(new ScoredTemplate(template, score));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble((ScoredTemplate v) -> -v.score)
                        .thenComparing(v -> v.template.id()))
                .limit(Math.max(0, picks))
                .map(v -> v.template)
                .toList();
    }


    public List<AbilityTemplate> selectTopWithInterferenceShuffle(List<AbilityTemplate> templates,
                                                                  ArtifactGenome genome,
                                                                  int picks,
                                                                  long deterministicSeed,
                                                                  double shortlistThreshold,
                                                                  int shortlistCap) {
        long start = System.nanoTime();
        Map<String, Double> scoreByAbility = fieldAdjustedScores(templates, genome, scoringMode);
        scoringCalls.incrementAndGet();
        totalScoringNanos.addAndGet(System.nanoTime() - start);

        List<ScoredTemplate> scored = new ArrayList<>(templates.size());
        for (AbilityTemplate template : templates) {
            scored.add(new ScoredTemplate(template, scoreByAbility.getOrDefault(template.id(), 0.0D) * triggerEfficiencyByAbility.getOrDefault(template.id(), 1.0D)));
        }
        scored.sort(Comparator.comparingDouble((ScoredTemplate v) -> -v.score)
                .thenComparing(v -> v.template.id()));

        List<AbilityTemplate> picked = new ArrayList<>();
        Random random = new Random(deterministicSeed);
        List<ScoredTemplate> remaining = new ArrayList<>(scored);
        int target = Math.max(0, picks);
        while (picked.size() < target && !remaining.isEmpty()) {
            ScoredTemplate selected = selectNearTop(remaining, random, shortlistThreshold, shortlistCap);
            picked.add(selected.template());
            remaining.removeIf(v -> v.template().id().equals(selected.template().id()));
        }
        return picked;
    }

    private ScoredTemplate selectNearTop(List<ScoredTemplate> sortedRemaining, Random random, double shortlistThreshold, int shortlistCap) {
        ScoredTemplate top = sortedRemaining.get(0);
        if (sortedRemaining.size() == 1 || top.score() <= 0.0D) {
            return top;
        }
        double threshold = Math.max(0.90D, Math.min(0.97D, shortlistThreshold));
        int cap = Math.max(2, Math.min(3, shortlistCap));
        double floor = top.score() * threshold;

        List<ScoredTemplate> shortlist = new ArrayList<>();
        for (ScoredTemplate candidate : sortedRemaining) {
            if (candidate.score() >= floor) {
                shortlist.add(candidate);
            }
            if (shortlist.size() >= cap) {
                break;
            }
        }
        if (shortlist.size() <= 1) {
            return top;
        }

        double total = 0.0D;
        for (ScoredTemplate candidate : shortlist) {
            total += Math.max(1.0E-6D, candidate.score());
        }
        double roll = random.nextDouble() * total;
        for (ScoredTemplate candidate : shortlist) {
            roll -= Math.max(1.0E-6D, candidate.score());
            if (roll <= 0.0D) {
                return candidate;
            }
        }
        return shortlist.get(shortlist.size() - 1);
    }

    public Map<String, Double> scoresByMode(ArtifactGenome genome, ScoringMode mode) {
        GenomeVector genomeVector = GenomeVector.fromGenome(genome);
        return switch (mode) {
            case BASELINE -> computeBaselineScores(genome, genomeVector);
            case PROJECTION_NO_CACHE -> computeProjectionWithoutCache(genomeVector);
            case PROJECTION_WITH_CACHE -> computeProjectionWithCache(genomeVector);
        };
    }

    public Map<String, Double> fieldAdjustedScores(List<AbilityTemplate> templates, ArtifactGenome genome, ScoringMode mode) {
        Map<String, Double> base = scoresByMode(genome, mode);
        Map<String, Double> adjusted = new HashMap<>(base.size());
        for (AbilityTemplate template : templates) {
            TraitInterferenceSnapshot snapshot = fieldMatrix.evaluate(template, genome);
            double value = base.getOrDefault(template.id(), 0.0D);
            adjusted.put(template.id(), value * (1.0D + snapshot.averageModifier()));
        }
        return adjusted;
    }

    public TraitInterferenceSnapshot summarizeInterference(List<AbilityTemplate> templates, ArtifactGenome genome, ScoringMode mode) {
        if (templates.isEmpty()) {
            return new TraitInterferenceSnapshot(0.0D, 0.5D, 0.5D, List.of());
        }
        Map<String, Double> adjusted = fieldAdjustedScores(templates, genome, mode);
        AbilityTemplate best = templates.stream()
                .max(Comparator.comparingDouble(t -> adjusted.getOrDefault(t.id(), 0.0D)))
                .orElse(templates.get(0));
        return fieldMatrix.evaluate(best, genome);
    }

    public void setScoringMode(ScoringMode scoringMode) {
        this.scoringMode = scoringMode == null ? ScoringMode.PROJECTION_WITH_CACHE : scoringMode;
    }

    public ScoringMode scoringMode() {
        return scoringMode;
    }

    public void resetStats() {
        scoringCalls.set(0L);
        totalScoringNanos.set(0L);
        projectionCache.clear();
    }

    public double score(AbilityTemplate template, ArtifactGenome genome) {
        Map<String, Double> scores = scoresByMode(genome, scoringMode);
        return scores.getOrDefault(template.id(), 0.0D);
    }

    public Map<GenomeTrait, Double> weightsFor(AbilityTemplate template) {
        return Map.copyOf(abilityWeights.getOrDefault(template.id(), inferByFamily(template.family())));
    }

    public TraitProjectionStats statsSnapshot() {
        long calls = Math.max(1L, scoringCalls.get());
        double averageMicros = totalScoringNanos.get() / (double) calls / 1_000.0D;
        long hits = projectionCache.hits();
        long misses = projectionCache.misses();
        double hitRate = (hits + misses) == 0 ? 0.0D : (double) hits / (hits + misses);
        double estimatedSpeedup = 1.0D + (hitRate * 2.5D);
        return new TraitProjectionStats(
                scoringMode != ScoringMode.BASELINE,
                scoringMode,
                scoringCalls.get(),
                hits,
                misses,
                projectionCache.size(),
                projectionCache.capacity(),
                projectionCache.evictions(),
                projectionMatrix.abilityCount(),
                projectionMatrix.dimensions(),
                averageMicros,
                estimatedSpeedup);
    }


    long cacheHits() {
        return projectionCache.hits();
    }

    long cacheMisses() {
        return projectionCache.misses();
    }

    private Map<String, Double> computeProjectionWithCache(GenomeVector genomeVector) {
        ProjectionCacheKey key = ProjectionCacheKey.from(genomeVector, 0L);
        return projectionCache.get(key)
                .map(GenomeProjection::abilityScores)
                .orElseGet(() -> computeAndCacheProjection(key, genomeVector));
    }

    private Map<String, Double> computeProjectionWithoutCache(GenomeVector genomeVector) {
        Map<String, Double> baseScores = projectionMatrix.scoreAll(genomeVector);
        Map<String, Double> withInteractions = new HashMap<>(baseScores.size());
        for (Map.Entry<String, Double> entry : baseScores.entrySet()) {
            withInteractions.put(entry.getKey(), entry.getValue() + interactionMatrix.interactionScore(entry.getKey(), genomeVector));
        }
        return withInteractions;
    }

    private Map<String, Double> computeBaselineScores(ArtifactGenome genome, GenomeVector genomeVector) {
        Map<String, Double> out = new HashMap<>(abilityWeights.size());
        for (Map.Entry<String, EnumMap<GenomeTrait, Double>> entry : abilityWeights.entrySet()) {
            double score = 0.0D;
            for (Map.Entry<GenomeTrait, Double> w : entry.getValue().entrySet()) {
                score += genome.trait(w.getKey()) * w.getValue();
            }
            score += interactionMatrix.interactionScore(entry.getKey(), genomeVector);
            out.put(entry.getKey(), score);
        }
        return out;
    }

    private Map<String, Double> computeAndCacheProjection(ProjectionCacheKey key, GenomeVector genomeVector) {
        Map<String, Double> withInteractions = computeProjectionWithoutCache(genomeVector);
        projectionCache.put(new GenomeProjection(key, withInteractions, 0L));
        return withInteractions;
    }

    private void registerDefaults(List<AbilityTemplate> templates) {
        for (AbilityTemplate template : templates) {
            abilityWeights.put(template.id(), inferByFamily(template.family()));
        }

        abilityWeights.put("precision.echo_locator", weights(GenomeTrait.PRECISION_AFFINITY, 0.62D, GenomeTrait.RESONANCE, 0.33D, GenomeTrait.STABILITY, 0.10D));
        abilityWeights.put("precision.vein_whisper", weights(GenomeTrait.PRECISION_AFFINITY, 0.58D, GenomeTrait.RESONANCE, 0.41D, GenomeTrait.CHAOS_AFFINITY, -0.10D));
        abilityWeights.put("mobility.footprint_memory", weights(GenomeTrait.MOBILITY_AFFINITY, 0.61D, GenomeTrait.RESONANCE, 0.35D, GenomeTrait.STABILITY, 0.22D));
        abilityWeights.put("survival.gentle_harvest", weights(GenomeTrait.SURVIVAL_INSTINCT, 0.60D, GenomeTrait.STABILITY, 0.40D, GenomeTrait.VOLATILITY, -0.15D));
        abilityWeights.put("chaos.witness", weights(GenomeTrait.CHAOS_AFFINITY, 0.62D, GenomeTrait.RESONANCE, 0.42D, GenomeTrait.STABILITY, 0.06D));
        abilityWeights.put("consistency.buried_memory", weights(GenomeTrait.STABILITY, 0.58D, GenomeTrait.RESONANCE, 0.36D, GenomeTrait.MOBILITY_AFFINITY, 0.14D));

        Map<String, EnumMap<GenomeTrait, Double>> filtered = new HashMap<>();
        for (AbilityTemplate template : templates) {
            EnumMap<GenomeTrait, Double> templateWeights = abilityWeights.getOrDefault(template.id(), inferByFamily(template.family()));
            filtered.put(template.id(), templateWeights);
            triggerEfficiencyByAbility.put(template.id(), efficiencyWeight(template.metadata()));
            projectionMatrix.register(template.id(), AbilityTraitVector.fromWeights(template.id(), templateWeights));
        }
        abilityWeights.clear();
        abilityWeights.putAll(filtered);
    }


    private double efficiencyWeight(AbilityMetadata metadata) {
        if (metadata == null) {
            return 1.0D;
        }
        double efficiency = metadata.triggerEfficiency();
        return Math.max(0.82D, Math.min(1.22D, 0.9D + (efficiency * 0.08D)));
    }

    private EnumMap<GenomeTrait, Double> inferByFamily(AbilityFamily family) {
        return switch (family) {
            case PRECISION -> weights(GenomeTrait.PRECISION_AFFINITY, 0.58D, GenomeTrait.RESONANCE, 0.26D, GenomeTrait.VOLATILITY, -0.14D);
            case BRUTALITY -> weights(GenomeTrait.KINETIC_BIAS, 0.50D, GenomeTrait.VOLATILITY, 0.30D, GenomeTrait.STABILITY, -0.16D);
            case SURVIVAL -> weights(GenomeTrait.SURVIVAL_INSTINCT, 0.62D, GenomeTrait.STABILITY, 0.34D, GenomeTrait.CHAOS_AFFINITY, -0.12D);
            case MOBILITY -> weights(GenomeTrait.MOBILITY_AFFINITY, 0.60D, GenomeTrait.KINETIC_BIAS, 0.32D, GenomeTrait.STABILITY, -0.08D);
            case CHAOS -> weights(GenomeTrait.CHAOS_AFFINITY, 0.70D, GenomeTrait.VOLATILITY, 0.35D, GenomeTrait.STABILITY, -0.24D);
            case CONSISTENCY -> weights(GenomeTrait.STABILITY, 0.66D, GenomeTrait.RESONANCE, 0.30D, GenomeTrait.VOLATILITY, -0.22D);
        };
    }

    private EnumMap<GenomeTrait, Double> weights(Object... values) {
        EnumMap<GenomeTrait, Double> out = new EnumMap<>(GenomeTrait.class);
        for (int i = 0; i < values.length; i += 2) {
            GenomeTrait trait = (GenomeTrait) values[i];
            Double weight = (Double) values[i + 1];
            out.put(trait, weight);
        }
        return out;
    }

    private record ScoredTemplate(AbilityTemplate template, double score) {
    }
}
