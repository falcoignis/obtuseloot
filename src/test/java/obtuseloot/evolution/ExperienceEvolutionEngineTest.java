package obtuseloot.evolution;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeResolver;
import obtuseloot.abilities.genome.GenomeTrait;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceEvolutionEngineTest {
    @Test
    void fitnessSignalsAreTrackedAndEvaluated() {
        ArtifactUsageProfile profile = new ArtifactUsageProfile();
        long start = 1_000_000L;
        profile.markCreated(start);
        profile.recordUse(start + 3_600_000L);
        profile.recordKill(start + 3_600_000L);
        profile.recordFusion(start + 3_600_000L);
        profile.recordAwakening(start + 3_600_000L);
        profile.recordDiscard(start + 3_600_000L);

        assertTrue(profile.usageFrequency() > 0.0D);
        assertEquals(1.0D, profile.killParticipation(), 1.0E-9D);
        assertTrue(profile.lifetimeHours() >= 1.0D);
        assertTrue(profile.discardRate() > 0.0D);
        assertEquals(1.0D, profile.fusionParticipation(), 1.0E-9D);
        assertEquals(1.0D, profile.awakeningRate(), 1.0E-9D);

        double fitness = new ArtifactFitnessEvaluator().evaluate(profile);
        assertTrue(fitness > 0.0D);
    }

    @Test
    void genomeDistributionAdjustsWithinEnvironmentBoundedRange() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        ArtifactFitnessEvaluator evaluator = new ArtifactFitnessEvaluator();
        ExperienceEvolutionEngine engine = new ExperienceEvolutionEngine(tracker, evaluator);
        long seed = 77L;

        ArtifactUsageProfile profile = tracker.profileForSeed(seed);
        long t = 100L;
        profile.markCreated(t);
        for (int i = 0; i < 100; i++) {
            t += 10_000L;
            profile.recordUse(t);
            if (i < 70) profile.recordKill(t);
            if (i < 15) profile.recordDiscard(t);
            if (i < 20) profile.recordFusion(t);
            if (i < 10) profile.recordAwakening(t);
        }

        ArtifactGenome base = new GenomeResolver().resolve(seed);
        ArtifactGenome adjusted = engine.applyExperienceFeedback(base, seed);

        for (GenomeTrait trait : GenomeTrait.values()) {
            double b = base.trait(trait);
            double a = adjusted.trait(trait);
            if (b > 0.0D) {
                double relative = Math.abs(a - b) / b;
                assertTrue(relative <= 0.300001D, "Trait adjusted beyond expected environment-adjusted range for " + trait);
            }
        }
    }
}
