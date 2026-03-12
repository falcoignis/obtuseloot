package obtuseloot.evolution;

import obtuseloot.abilities.AbilityExecutionStatus;
import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityOutcomeType;
import obtuseloot.abilities.AbilityTrigger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactUtilityFitnessModelTest {

    @Test
    void meaningfulOutcomesScoreHigherThanNoOps() {
        ArtifactUsageProfile profile = new ArtifactUsageProfile();
        ArtifactUsageProfile noOpOnly = new ArtifactUsageProfile();
        long now = 1_000L;
        profile.recordUtilityOutcome(outcome(now, true, false, AbilityOutcomeType.WORLD_INTERACTION, true, "ctx"));
        profile.recordUtilityOutcome(outcome(now + 1, true, false, AbilityOutcomeType.NAVIGATION_HINT, true, "ctx"));
        profile.recordUtilityOutcome(outcome(now + 2, false, true, AbilityOutcomeType.FLAVOR_ONLY, false, "ctx"));
        noOpOnly.recordUtilityOutcome(outcome(now, false, true, AbilityOutcomeType.FLAVOR_ONLY, false, "ctx"));

        assertTrue(profile.validatedUtilityScore() > noOpOnly.validatedUtilityScore());
        assertTrue(profile.meaningfulOutcomeRate() > profile.averageNoOpRate());
    }

    @Test
    void repeatedNoOpsReduceUtilityAndIncreaseSpamPenalty() {
        ArtifactUsageProfile profile = new ArtifactUsageProfile();
        long now = 2_000L;
        for (int i = 0; i < 12; i++) {
            profile.recordUtilityOutcome(outcome(now + i, false, true, AbilityOutcomeType.FLAVOR_ONLY, false, "ambient"));
        }

        assertTrue(profile.validatedUtilityScore() < 0.0D);
        assertTrue(profile.averageSpamPenalty() > 0.2D);
    }

    @Test
    void redundantLowValueOutcomesHaveDiminishingReturns() {
        ArtifactUsageProfile profile = new ArtifactUsageProfile();
        long now = 3_000L;
        profile.recordUtilityOutcome(outcome(now, true, false, AbilityOutcomeType.INFORMATION, false, "hint"));
        double first = profile.validatedUtilityScore();
        for (int i = 0; i < 5; i++) {
            profile.recordUtilityOutcome(outcome(now + i + 1, true, false, AbilityOutcomeType.INFORMATION, false, "hint"));
        }
        double growth = profile.validatedUtilityScore() - first;
        assertTrue(growth < first);
        assertTrue(profile.averageRedundancyPenalty() > 0.15D);
    }

    @Test
    void lowVolumeHighValueOutperformsHighVolumeLowValueInFitness() {
        ArtifactUsageProfile noisy = new ArtifactUsageProfile();
        ArtifactUsageProfile precise = new ArtifactUsageProfile();
        long t = 4_000L;

        for (int i = 0; i < 25; i++) {
            noisy.recordUse(t + i);
            noisy.recordUtilityOutcome(outcome(t + i, false, true, AbilityOutcomeType.FLAVOR_ONLY, false, "ambient"));
        }
        for (int i = 0; i < 4; i++) {
            precise.recordUse(t + i);
            precise.recordUtilityOutcome(outcome(t + i, true, false, AbilityOutcomeType.NAVIGATION_HINT, true, "intent"));
        }

        ArtifactFitnessEvaluator evaluator = new ArtifactFitnessEvaluator();
        assertTrue(evaluator.evaluate(precise) > evaluator.evaluate(noisy));
    }

    @Test
    void intentionalContextIncreasesUtilityWeightAndDensity() {
        ArtifactUsageProfile profile = new ArtifactUsageProfile();
        long now = 5_000L;
        profile.recordUtilityOutcome(outcome(now, true, false, AbilityOutcomeType.STRUCTURE_SENSE, false, "intent"));
        double intentionalScore = profile.validatedUtilityScore();

        ArtifactUsageProfile passive = new ArtifactUsageProfile();
        passive.recordUtilityOutcome(outcome(now, false, false, AbilityOutcomeType.STRUCTURE_SENSE, false, "ambient"));

        assertTrue(intentionalScore > passive.validatedUtilityScore());
        assertTrue(profile.utilityDensity() > passive.utilityDensity());
    }

    @Test
    void utilityDensityAndBudgetEfficiencyAreComputed() {
        ArtifactUsageProfile profile = new ArtifactUsageProfile();
        long now = 6_000L;
        profile.recordUtilityOutcome(outcome(now, true, false, AbilityOutcomeType.CROP_REPLANT, true, "farm"));
        profile.recordUtilityOutcome(outcome(now + 1, true, false, AbilityOutcomeType.CROP_REPLANT, true, "farm"));

        assertTrue(profile.utilityDensity() > 0.0D);
        assertEquals(profile.utilityDensity(), profile.utilityBudgetEfficiency(), 1.0E-9D);
    }

    @Test
    void activityVolumeAloneDoesNotDominateSelectionSignal() {
        ArtifactUsageProfile highActivityNoValue = new ArtifactUsageProfile();
        ArtifactUsageProfile modestActivityValue = new ArtifactUsageProfile();
        long now = 7_000L;

        for (int i = 0; i < 40; i++) {
            highActivityNoValue.recordUse(now + i);
            highActivityNoValue.recordUtilityOutcome(outcome(now + i, false, true, AbilityOutcomeType.FLAVOR_ONLY, false, "ambient"));
        }

        for (int i = 0; i < 6; i++) {
            modestActivityValue.recordUse(now + i);
            modestActivityValue.recordUtilityOutcome(outcome(now + i, true, false, AbilityOutcomeType.WORLD_INTERACTION, true, "intent"));
        }

        ArtifactFitnessEvaluator evaluator = new ArtifactFitnessEvaluator();
        assertTrue(evaluator.evaluate(modestActivityValue) > evaluator.evaluate(highActivityNoValue));
    }

    @Test
    void triggerBudgetCostPenalizesExpensiveLowValueSignals() {
        ArtifactUsageProfile expensive = new ArtifactUsageProfile();
        ArtifactUsageProfile efficient = new ArtifactUsageProfile();
        long now = 8_000L;

        for (int i = 0; i < 5; i++) {
            expensive.recordUtilityOutcome(new UtilityOutcomeRecord("a", AbilityMechanic.PULSE, AbilityTrigger.ON_WORLD_SCAN,
                    AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.INFORMATION, true, false, 0.8D, 2.8D, "ambient", now + i));
            efficient.recordUtilityOutcome(new UtilityOutcomeRecord("b", AbilityMechanic.HARVEST_RELAY, AbilityTrigger.ON_BLOCK_HARVEST,
                    AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.CROP_REPLANT, true, true, 1.2D, 0.7D, "intent", now + i));
        }

        assertTrue(efficient.utilityDensity() > expensive.utilityDensity());
    }

    @Test
    void legacyActivityActsAsSecondaryConfidenceOnly() {
        ArtifactUsageProfile highUtilityLowVolume = new ArtifactUsageProfile();
        ArtifactUsageProfile lowUtilityHighVolume = new ArtifactUsageProfile();
        long now = 9_000L;

        for (int i = 0; i < 5; i++) {
            highUtilityLowVolume.recordUse(now + i);
            highUtilityLowVolume.recordUtilityOutcome(outcome(now + i, true, false, AbilityOutcomeType.WORLD_INTERACTION, true, "intent"));
        }

        for (int i = 0; i < 70; i++) {
            lowUtilityHighVolume.recordUse(now + i);
        }
        for (int i = 0; i < 8; i++) {
            lowUtilityHighVolume.recordUtilityOutcome(outcome(now + i, false, true, AbilityOutcomeType.FLAVOR_ONLY, false, "ambient"));
        }

        ArtifactFitnessEvaluator evaluator = new ArtifactFitnessEvaluator();
        assertTrue(evaluator.evaluate(highUtilityLowVolume) > evaluator.evaluate(lowUtilityHighVolume));
    }

    private UtilityOutcomeRecord outcome(long t,
                                         boolean intentional,
                                         boolean noOp,
                                         AbilityOutcomeType type,
                                         boolean meaningful,
                                         String source) {
        return new UtilityOutcomeRecord(
                "ability-1",
                AbilityMechanic.PULSE,
                AbilityTrigger.ON_WORLD_SCAN,
                noOp ? AbilityExecutionStatus.NO_OP : AbilityExecutionStatus.SUCCESS,
                type,
                meaningful,
                intentional,
                intentional ? 1.1D : 0.7D,
                intentional ? 0.8D : 1.3D,
                source,
                t
        );
    }
}
