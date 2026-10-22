package obtuseloot.abilities;

import obtuseloot.analytics.RelativeDiversityHealth;
import obtuseloot.evolution.ExperienceEvolutionEngine;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** One-layer ablation. Other selection aids remain; this is not proof of emergence. */
class TopThreeCompressionAblationTest {
    @Test
    void samplerHonorsInputWeightIntervalsWithoutFlatteningOrMutation() {
        var generator = current(new AbilityRegistry(), null);
        double[] weights = {9.0D, 1.0D, 1.0D, 1.0D};
        assertEquals(0, generator.sampleTemplateIndex(weights, draw(0.65D)));
        assertArrayEquals(new double[]{9.0D, 1.0D, 1.0D, 1.0D}, weights);
        assertEquals(1, generator.sampleTemplateIndex(weights, draw(0.76D)));
        assertEquals(3, generator.sampleTemplateIndex(weights, draw(0.99D)));
        // Swapping the favored input moves the same draw to the new interval.
        assertEquals(3, generator.sampleTemplateIndex(new double[]{1, 1, 1, 9}, draw(0.65D)));
        assertEquals(0, generator.sampleTemplateIndex(new double[]{1}, draw(0.99D)));
        assertEquals(1, generator.sampleTemplateIndex(new double[]{1, 3}, draw(0.5D)));
    }

    @Test
    void legacyControlReallyChangesSelectionAndFlattensSmallCategories() {
        var legacy = new LegacyGenerator(new AbilityRegistry(), null);
        double[] weights = {9, 1, 1, 1};
        assertEquals(2, legacy.sampleTemplateIndex(weights, draw(0.65D)));
        assertEquals(1, legacy.changedWeights);
        assertEquals(1, legacy.changedSelections);
        assertArrayEquals(new double[]{9, 1, 1, 1}, weights, "Control must not corrupt caller inputs");
        double[] five = {9, 4, 3, 2, 1};
        compressAsBefore(five);
        assertArrayEquals(new double[]{3.8D, 3.8D, 3.8D, 3.8D, 3.8D}, five, 1.0E-12D,
                "The old 50% target is unattainable for five positive weights, so it goes uniform");
    }

    @Test
    void seededOutcomeFeedbackRetainsRelativeHealthWithCompressionAblated() {
        var legacyHolder = new AtomicReference<LegacyGenerator>();
        var before = SeededEcosystemHealthTest.simulate((registry, feedback) -> {
            var generator = new LegacyGenerator(registry, feedback);
            legacyHolder.set(generator);
            return generator;
        });
        var after = SeededEcosystemHealthTest.simulate(TopThreeCompressionAblationTest::current);
        var replay = SeededEcosystemHealthTest.simulate(TopThreeCompressionAblationTest::current);
        assertEquals(after, replay, "Treatment must replay with isolated state and identical seed");
        var baselineHealth = RelativeDiversityHealth.assess(before, SeededEcosystemHealthTest.BASELINE_WINDOWS,
                SeededEcosystemHealthTest.MAXIMUM_RELATIVE_DROP);
        var ablatedHealth = RelativeDiversityHealth.assess(after, SeededEcosystemHealthTest.BASELINE_WINDOWS,
                SeededEcosystemHealthTest.MAXIMUM_RELATIVE_DROP);
        var legacy = legacyHolder.get();
        assertTrue(legacy.changedWeights > 0, "Ablation scenario must actually exercise the removed aid");
        assertTrue(legacy.changedSelections > 0, "Control must affect actual generator decisions");
        assertTrue(baselineHealth.healthy(), () -> "Legacy control health: " + baselineHealth);
        assertTrue(ablatedHealth.healthy(), () -> "Ablated ecosystem health: " + ablatedHealth);
        System.out.println("TOP_THREE_ABLATION legacy=" + baselineHealth + " ablated=" + ablatedHealth
                + " changedWeights=" + legacy.changedWeights + " changedSelections=" + legacy.changedSelections
                + " legacyWindows=" + before + " ablatedWindows=" + after);
    }

    private static ProceduralAbilityGenerator current(AbilityRegistry registry, ExperienceEvolutionEngine feedback) {
        return new ProceduralAbilityGenerator(registry, null, null, null,
                feedback, true, ScoringMode.PROJECTION_WITH_CACHE, new AbilityDiversityIndex());
    }

    private static Random draw(double value) {
        return new Random(0L) {
            @Override public double nextDouble() { return value; }
        };
    }

    private static final class LegacyGenerator extends ProceduralAbilityGenerator {
        private int changedWeights;
        private int changedSelections;

        private LegacyGenerator(AbilityRegistry registry, ExperienceEvolutionEngine feedback) {
            super(registry, null, null, null, feedback, true,
                    ScoringMode.PROJECTION_WITH_CACHE, new AbilityDiversityIndex());
        }

        @Override int sampleTemplateIndex(double[] weights, Random random) {
            double[] compressed = weights.clone();
            compressAsBefore(compressed);
            if (!Arrays.equals(weights, compressed)) changedWeights++;
            // Consume exactly the original one RNG draw in either treatment.
            double roll = random.nextDouble();
            int before = super.sampleTemplateIndex(compressed, draw(roll));
            int after = super.sampleTemplateIndex(weights.clone(), draw(roll));
            if (before != after) changedSelections++;
            return before;
        }
    }

    // Test-only control copied from 26c2f9816e1e1324ef3d176bcba8eead8974dd5e.
    // Do not ship this policy or expose it as a live configuration option.
    private static void compressAsBefore(double[] weights) {
        double total = 0.0D;
        for (double weight : weights) total += weight;
        if (weights.length <= 3 || total <= 0.0D || topThreeShare(weights, total) <= 0.55D) return;
        double mean = total / weights.length;
        double low = 0.0D, high = 1.0D;
        for (int i = 0; i < 24; i++) {
            double mid = (low + high) / 2.0D;
            double[] candidate = new double[weights.length];
            double candidateTotal = 0.0D;
            for (int j = 0; j < weights.length; j++) {
                candidate[j] = weights[j] + ((mean - weights[j]) * mid);
                candidateTotal += candidate[j];
            }
            if (topThreeShare(candidate, candidateTotal) > 0.50D) low = mid;
            else high = mid;
        }
        for (int i = 0; i < weights.length; i++) weights[i] += (mean - weights[i]) * high;
    }

    private static double topThreeShare(double[] weights, double total) {
        double first = 0.0D, second = 0.0D, third = 0.0D;
        for (double weight : weights) {
            if (weight > first) { third = second; second = first; first = weight; }
            else if (weight > second) { third = second; second = weight; }
            else if (weight > third) third = weight;
        }
        return (first + second + third) / total;
    }
}
