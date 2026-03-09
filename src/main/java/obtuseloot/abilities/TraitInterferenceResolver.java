package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class TraitInterferenceResolver {
    private static final int DEFAULT_CACHE_SIZE = 25_000;

    private final Map<String, EnumMap<GenomeTrait, Double>> abilityWeights = new HashMap<>();
    private final TraitProjectionMatrix projectionMatrix = new TraitProjectionMatrix();
    private final InteractionProjectionMatrix interactionMatrix = new InteractionProjectionMatrix();
    private final ProjectionCache projectionCache;
    private final AtomicLong scoringCalls = new AtomicLong();
    private final AtomicLong totalScoringNanos = new AtomicLong();

    public TraitInterferenceResolver(List<AbilityTemplate> templates) {
        this(templates, DEFAULT_CACHE_SIZE);
    }

    TraitInterferenceResolver(List<AbilityTemplate> templates, int cacheSize) {
        this.projectionCache = new ProjectionCache(cacheSize);
        registerDefaults(templates);
    }

    public List<AbilityTemplate> selectTop(List<AbilityTemplate> templates, ArtifactGenome genome, int picks) {
        GenomeVector genomeVector = GenomeVector.fromGenome(genome);
        ProjectionCacheKey key = ProjectionCacheKey.from(genomeVector, 0L);
        Map<String, Double> scoreByAbility = projectionCache.get(key)
                .map(GenomeProjection::abilityScores)
                .orElseGet(() -> computeAndCacheProjection(key, genomeVector));

        List<ScoredTemplate> scored = new ArrayList<>(templates.size());
        for (AbilityTemplate template : templates) {
            double score = scoreByAbility.getOrDefault(template.id(), 0.0D);
            scored.add(new ScoredTemplate(template, score));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble((ScoredTemplate v) -> -v.score)
                        .thenComparing(v -> v.template.id()))
                .limit(Math.max(0, picks))
                .map(v -> v.template)
                .toList();
    }

    public double score(AbilityTemplate template, ArtifactGenome genome) {
        scoringCalls.incrementAndGet();
        GenomeVector vector = GenomeVector.fromGenome(genome);
        long start = System.nanoTime();
        double finalScore = scoreForTemplate(template.id(), vector);
        totalScoringNanos.addAndGet(System.nanoTime() - start);
        return finalScore;
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
                true,
                scoringCalls.get(),
                hits,
                misses,
                projectionCache.size(),
                projectionCache.capacity(),
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

    private Map<String, Double> computeAndCacheProjection(ProjectionCacheKey key, GenomeVector genomeVector) {
        long start = System.nanoTime();
        Map<String, Double> baseScores = projectionMatrix.scoreAll(genomeVector);
        Map<String, Double> withInteractions = new HashMap<>(baseScores.size());
        for (Map.Entry<String, Double> entry : baseScores.entrySet()) {
            withInteractions.put(entry.getKey(), entry.getValue() + interactionMatrix.interactionScore(entry.getKey(), genomeVector));
        }
        long elapsed = System.nanoTime() - start;
        scoringCalls.incrementAndGet();
        totalScoringNanos.addAndGet(elapsed);
        projectionCache.put(new GenomeProjection(key, withInteractions, elapsed));
        return withInteractions;
    }

    private double scoreForTemplate(String abilityId, GenomeVector genomeVector) {
        AbilityTraitVector abilityVector = projectionMatrix.forAbility(abilityId);
        if (abilityVector == null) {
            return 0.0D;
        }
        double base = genomeVector.dot(abilityVector);
        double interaction = interactionMatrix.interactionScore(abilityId, genomeVector);
        return base + interaction;
    }

    private void registerDefaults(List<AbilityTemplate> templates) {
        for (AbilityTemplate template : templates) {
            abilityWeights.put(template.id(), inferByFamily(template.family()));
        }

        abilityWeights.put("precision.sigil", weights(GenomeTrait.PRECISION_AFFINITY, 0.70D, GenomeTrait.RESONANCE, 0.40D, GenomeTrait.VOLATILITY, -0.20D));
        abilityWeights.put("precision.gambit", weights(GenomeTrait.PRECISION_AFFINITY, 0.64D, GenomeTrait.KINETIC_BIAS, 0.30D, GenomeTrait.STABILITY, 0.18D));
        abilityWeights.put("precision.refractor", weights(GenomeTrait.PRECISION_AFFINITY, 0.58D, GenomeTrait.RESONANCE, 0.52D, GenomeTrait.VOLATILITY, -0.16D));

        abilityWeights.put("mobility.wake", weights(GenomeTrait.MOBILITY_AFFINITY, 0.65D, GenomeTrait.KINETIC_BIAS, 0.45D, GenomeTrait.STABILITY, -0.10D));
        abilityWeights.put("survival.ward", weights(GenomeTrait.SURVIVAL_INSTINCT, 0.72D, GenomeTrait.STABILITY, 0.36D, GenomeTrait.VOLATILITY, -0.22D));
        abilityWeights.put("chaos.spore", weights(GenomeTrait.CHAOS_AFFINITY, 0.80D, GenomeTrait.VOLATILITY, 0.46D, GenomeTrait.STABILITY, -0.40D));
        abilityWeights.put("chaos.paradox", weights(GenomeTrait.CHAOS_AFFINITY, 0.88D, GenomeTrait.RESONANCE, 0.30D, GenomeTrait.STABILITY, -0.30D));

        Map<String, EnumMap<GenomeTrait, Double>> filtered = new HashMap<>();
        for (AbilityTemplate template : templates) {
            EnumMap<GenomeTrait, Double> templateWeights = abilityWeights.getOrDefault(template.id(), inferByFamily(template.family()));
            filtered.put(template.id(), templateWeights);
            projectionMatrix.register(template.id(), AbilityTraitVector.fromWeights(template.id(), templateWeights));
        }
        abilityWeights.clear();
        abilityWeights.putAll(filtered);
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
