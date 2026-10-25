package obtuseloot.abilities;

import obtuseloot.analytics.RelativeDiversityHealth;
import obtuseloot.evolution.ExperienceEvolutionEngine;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Soft-cap ablation, pre-registered in docs/soft-cap-ablation.md: composite scores are sampled as measured. */
class SoftCapAblationTest {
    @Test
    void compositeScoresAreSampledWithoutTheMeanRelativeCap() {
        var registry = new AbilityRegistry();
        var generator = new ScoreNormalizationAblationTest.RecordingGenerator(registry, null);
        double[] scores = {1, 2, 9};
        double[] result = generator.categoryTemplateSamplingScores(registry.templates().subList(0, 3), scores);
        assertArrayEquals(new double[]{1, 2, 9}, result, "The leader keeps its measured score");
        assertNotSame(scores, result, "Cold balancing edits the returned array, so a varied category gets a copy");
        assertArrayEquals(new double[]{1, 2, 9}, scores, "Preparation must not mutate composite scores");
    }

    @Test
    void legacyControlRestoresTheRemovedCap() {
        var registry = new AbilityRegistry();
        var legacy = new CappedControl(registry, null);
        double[] result = legacy.categoryTemplateSamplingScores(registry.templates().subList(0, 3), new double[]{1, 2, 9});
        assertEquals(1.0D, result[0], 1.0E-12D);
        assertEquals(2.0D, result[1], 1.0E-12D);
        assertEquals(4.0D + 1.12D * Math.tanh(5.0D / 1.12D), result[2], 1.0E-12D, "The removed 1.28x-mean tanh cap");
        assertEquals(1, legacy.changedScores);
    }

    @Test
    void seededUtilityFeedbackComparesTheCapWithItsAblation() {
        var legacyHolder = new AtomicReference<CappedControl>();
        var currentHolder = new AtomicReference<ScoreNormalizationAblationTest.RecordingGenerator>();
        var replayHolder = new AtomicReference<ScoreNormalizationAblationTest.RecordingGenerator>();
        var before = SeededEcosystemHealthTest.simulate((registry, feedback) -> {
            var generator = new CappedControl(registry, feedback);
            legacyHolder.set(generator);
            return generator;
        });
        var after = SeededEcosystemHealthTest.simulate((registry, feedback) -> {
            var generator = new ScoreNormalizationAblationTest.RecordingGenerator(registry, feedback);
            currentHolder.set(generator);
            return generator;
        });
        var replay = SeededEcosystemHealthTest.simulate((registry, feedback) -> {
            var generator = new ScoreNormalizationAblationTest.RecordingGenerator(registry, feedback);
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
        assertTrue(legacy.changedScores > 0, "The restored cap must actually change scores");
        assertTrue(differentProfiles > 0, "Ablation must change actual generated profiles, not just diagnostics");
        var cappedHealth = RelativeDiversityHealth.assess(before, SeededEcosystemHealthTest.BASELINE_WINDOWS,
                SeededEcosystemHealthTest.MAXIMUM_RELATIVE_DROP);
        var ablatedHealth = RelativeDiversityHealth.assess(after, SeededEcosystemHealthTest.BASELINE_WINDOWS,
                SeededEcosystemHealthTest.MAXIMUM_RELATIVE_DROP);
        System.out.println("SOFT_CAP_ABLATION capped=" + cappedHealth + " ablated=" + ablatedHealth
                + " changedScores=" + legacy.changedScores + " differentProfiles=" + differentProfiles
                + " cappedWindows=" + before + " ablatedWindows=" + after);
        assertTrue(cappedHealth.healthy(), () -> "Capped health: " + cappedHealth);
        assertTrue(ablatedHealth.healthy(), () -> "Ablated health: " + ablatedHealth);
    }

    /** Test-only control: current scoring with the removed soft cap restored. */
    private static final class CappedControl extends ScoreNormalizationAblationTest.RecordingGenerator {
        int changedScores;

        CappedControl(AbilityRegistry registry, ExperienceEvolutionEngine feedback) {
            super(registry, feedback);
        }

        @Override double[] categoryTemplateSamplingScores(List<AbilityTemplate> templates, double[] scores) {
            double[] current = super.categoryTemplateSamplingScores(templates, scores);
            if (current == scores) return scores;
            double[] capped = LegacySoftCap.samplingScores(scores);
            if (!Arrays.equals(current, capped)) changedScores++;
            return capped;
        }
    }
}
