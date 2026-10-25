package obtuseloot.abilities;

import obtuseloot.analytics.RelativeDiversityHealth;
import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.ExperienceEvolutionEngine;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gamma/mean-blend ablation only. The soft cap was removed later (docs/soft-cap-ablation.md); both arms here
 * restore it from {@link LegacySoftCap} so this historical comparison is unchanged.
 */
class ScoreNormalizationAblationTest {
    @Test
    void rawScoresReachTheExistingSoftCapWithoutGammaOrMeanBlending() {
        var registry = new AbilityRegistry();
        var generator = new CappedGenerator(registry, null);
        double[] scores = {1, 2, 9};
        double[] result = generator.categoryTemplateSamplingScores(registry.templates().subList(0, 3), scores);
        assertEquals(1.0D, result[0], 1.0E-12D, "A below-mean score must not be raised toward the mean");
        assertEquals(2.0D, result[1], 1.0E-12D, "The gamma curve must not expand a lower score");
        double expectedCappedLeader = 4.0D + 1.12D * Math.tanh(5.0D / 1.12D);
        assertEquals(expectedCappedLeader, result[2], 1.0E-12D, "Keep the existing 1.28x-mean soft-cap formula");
        assertTrue(result[2] < scores[2], "This slice must still exercise the remaining soft cap");
        assertArrayEquals(new double[]{1, 2, 9}, scores, "Preparation must not mutate composite scores");
    }

    @Test
    void existingEmptySingletonUniformAndNonfiniteGuardsArePreserved() {
        var registry = new AbilityRegistry();
        var generator = new RecordingGenerator(registry, null);
        List<AbilityTemplate> three = registry.templates().subList(0, 3);
        double[] empty = {};
        double[] single = {9};
        double[] uniform = {2, 2, 2};
        double[] nan = {1, Double.NaN, 9};
        double[] infinite = {1, 2, Double.POSITIVE_INFINITY};
        assertSame(empty, generator.categoryTemplateSamplingScores(List.of(), empty));
        assertSame(single, generator.categoryTemplateSamplingScores(three.subList(0, 1), single));
        assertSame(uniform, generator.categoryTemplateSamplingScores(three, uniform));
        assertSame(nan, generator.categoryTemplateSamplingScores(three, nan));
        assertSame(infinite, generator.categoryTemplateSamplingScores(three, infinite));
    }

    @Test
    void legacyControlRestoresTheRemovedInflationBeforeTheSameSoftCap() {
        var registry = new AbilityRegistry();
        var current = new CappedGenerator(registry, null);
        var legacy = new LegacyGenerator(registry, null);
        List<AbilityTemplate> three = registry.templates().subList(0, 3);
        double[] scores = {1, 2, 9};
        double[] before = legacy.categoryTemplateSamplingScores(three, scores);
        double[] after = current.categoryTemplateSamplingScores(three, scores);
        assertEquals(2.05D, before[0], 1.0E-12D);
        assertTrue(before[0] > after[0] && before[1] > after[1]);
        assertEquals(1, legacy.changedScores);
        assertArrayEquals(new double[]{1, 2, 9}, scores);
    }

    @Test
    void seededUtilityFeedbackRetainsRelativeHealthWithNormalizationAblated() {
        var legacyHolder = new AtomicReference<LegacyGenerator>();
        var currentHolder = new AtomicReference<RecordingGenerator>();
        var replayHolder = new AtomicReference<RecordingGenerator>();
        var before = SeededEcosystemHealthTest.simulate((registry, feedback) -> {
            var generator = new LegacyGenerator(registry, feedback);
            legacyHolder.set(generator);
            return generator;
        });
        var after = SeededEcosystemHealthTest.simulate((registry, feedback) -> {
            var generator = new CappedGenerator(registry, feedback);
            currentHolder.set(generator);
            return generator;
        });
        var replay = SeededEcosystemHealthTest.simulate((registry, feedback) -> {
            var generator = new CappedGenerator(registry, feedback);
            replayHolder.set(generator);
            return generator;
        });
        var legacy = legacyHolder.get();
        var current = currentHolder.get();
        assertEquals(after, replay, "Occupancy windows must replay with isolated state");
        assertEquals(current.profiles, replayHolder.get().profiles, "Actual generated profiles must replay too");
        assertFalse(current.profiles.isEmpty());
        assertEquals(legacy.profiles.size(), current.profiles.size());
        long differentProfiles = IntStream.range(0, current.profiles.size())
                .filter(i -> !legacy.profiles.get(i).equals(current.profiles.get(i))).count();
        assertTrue(legacy.changedScores > 0, "The restored transform must actually change scores");
        assertTrue(differentProfiles > 0, "Ablation must change actual generated profiles, not just diagnostics");
        var baselineHealth = RelativeDiversityHealth.assess(before, SeededEcosystemHealthTest.BASELINE_WINDOWS,
                SeededEcosystemHealthTest.MAXIMUM_RELATIVE_DROP);
        var ablatedHealth = RelativeDiversityHealth.assess(after, SeededEcosystemHealthTest.BASELINE_WINDOWS,
                SeededEcosystemHealthTest.MAXIMUM_RELATIVE_DROP);
        assertTrue(baselineHealth.healthy(), () -> "Legacy normalization health: " + baselineHealth);
        assertTrue(ablatedHealth.healthy(), () -> "Ablated normalization health: " + ablatedHealth);
        System.out.println("SCORE_NORMALIZATION_ABLATION legacy=" + baselineHealth + " ablated=" + ablatedHealth
                + " changedScores=" + legacy.changedScores + " differentProfiles=" + differentProfiles
                + " legacyWindows=" + before + " ablatedWindows=" + after);
    }

    static class RecordingGenerator extends ProceduralAbilityGenerator {
        final List<List<String>> profiles = new ArrayList<>();

        RecordingGenerator(AbilityRegistry registry, ExperienceEvolutionEngine feedback) {
            super(registry, null, null, null, feedback, true,
                    ScoringMode.PROJECTION_WITH_CACHE, new AbilityDiversityIndex());
        }

        @Override public AbilityProfile generate(Artifact artifact, int stage, ArtifactMemoryProfile memory) {
            var profile = super.generate(artifact, stage, memory);
            profiles.add(profile.abilities().stream()
                    .map(ability -> ability.id() + "@" + ability.mechanic() + "@" + ability.trigger()).toList());
            return profile;
        }
    }

    /** Current scoring with the removed soft cap restored: the treatment arm of the original ablation. */
    private static class CappedGenerator extends RecordingGenerator {
        CappedGenerator(AbilityRegistry registry, ExperienceEvolutionEngine feedback) {
            super(registry, feedback);
        }

        @Override double[] categoryTemplateSamplingScores(List<AbilityTemplate> templates, double[] scores) {
            double[] current = super.categoryTemplateSamplingScores(templates, scores);
            return current == scores ? scores : LegacySoftCap.samplingScores(scores);
        }
    }

    private static final class LegacyGenerator extends CappedGenerator {
        int changedScores;

        LegacyGenerator(AbilityRegistry registry, ExperienceEvolutionEngine feedback) {
            super(registry, feedback);
        }

        @Override double[] categoryTemplateSamplingScores(List<AbilityTemplate> templates, double[] scores) {
            double[] current = super.categoryTemplateSamplingScores(templates, scores);
            double[] legacy = normalizeAsBefore(templates, scores);
            if (!Arrays.equals(current, legacy)) changedScores++;
            return legacy;
        }

        // Test-only control from 6e886ec742c697af34d46b802c79095ff659014c.
        // Restores gamma and mean blending before the same restored soft cap.
        private double[] normalizeAsBefore(List<AbilityTemplate> templates, double[] scores) {
            if (templates.isEmpty() || templates.size() <= 1) return scores;
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY, sum = 0.0D;
            for (double score : scores) {
                min = Math.min(min, score);
                max = Math.max(max, score);
                sum += score;
            }
            if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min) return scores;
            double mean = sum / scores.length;
            double span = Math.max(max - min, 1.0E-9D);
            double[] transformed = new double[scores.length];
            for (int i = 0; i < scores.length; i++) {
                double normalized = Math.max(0.0D, Math.min(1.0D, (scores[i] - min) / span));
                double curved = Math.pow(normalized, 1.0D / 2.50D);
                double expanded = min + curved * span;
                transformed[i] = expanded + (mean - expanded) * 0.35D;
            }
            return LegacySoftCap.apply(transformed, mean);
        }
    }
}
