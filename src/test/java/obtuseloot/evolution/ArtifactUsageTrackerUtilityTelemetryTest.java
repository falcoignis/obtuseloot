package obtuseloot.evolution;

import obtuseloot.abilities.AbilityExecutionStatus;
import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityOutcomeType;
import obtuseloot.abilities.AbilityTrigger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactUsageTrackerUtilityTelemetryTest {
    @Test void measuredBenefitAndRecoveryReachItemSelectionHistoryWithoutChangingUnmeasuredEffects() {
        var artifact = new obtuseloot.artifacts.Artifact(new java.util.UUID(0, 10), "diamond_boots");
        artifact.setArtifactSeed(-42);
        artifact.setArtifactStorageKey("artifact:" + new java.util.UUID(0, 42));
        var template = new obtuseloot.abilities.AbilityRegistry().templateById("mobility.double_jump");
        var definition = new obtuseloot.abilities.AbilityDefinition(template.id(), template.name(), template.family(), template.trigger(), template.mechanic(),
                "", "", "", "", "", "", java.util.List.of(), java.util.List.of(), template.metadata(), "1", "2", "3", "4", "5");
        var context = new obtuseloot.abilities.AbilityEventContext(template.trigger(), artifact, artifact.getItemReputation(), 1, "fixture",
                obtuseloot.abilities.AbilityRuntimeContext.intentional(obtuseloot.abilities.AbilitySource.OTHER), null, true);
        var result = obtuseloot.abilities.AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.WORLD_INTERACTION, .6, null, null);
        double oldDelta = new ArtifactUsageTracker().trackAbilityExecution(artifact, context, result, definition);
        var measured = result.withPerformance(new obtuseloot.abilities.AbilityPerformanceMeasurement(.6, 30)).withObservationKey("receipt");
        double delta = new ArtifactUsageTracker().trackAbilityExecution(artifact, context, measured, definition);
        assertEquals(oldDelta * .6 / 2.5, delta, 1e-12);
        var saved = UtilityHistoryRollup.parse(artifact.getLastUtilityHistory()).signalByMechanicTrigger().get("MOVEMENT_ECHO@ON_AIR_JUMP");
        assertEquals(delta, saved.validatedUtility(), 1e-12);
        assertTrue(new obtuseloot.abilities.AbilityOutcomeObservation(context, measured, definition, delta).liveMeasured());
    }

    @Test
    void rollupsExposeUtilityDensityAndHighVolumeLowValueMechanics() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        ArtifactUsageProfile profile = tracker.profileForSeed(123L);

        for (int i = 0; i < 10; i++) {
            profile.recordUtilityOutcome(new UtilityOutcomeRecord(
                    "a",
                    AbilityMechanic.PULSE,
                    AbilityTrigger.ON_WORLD_SCAN,
                    AbilityExecutionStatus.NO_OP,
                    AbilityOutcomeType.FLAVOR_ONLY,
                    false,
                    false,
                    0.6D,
                    1.4D,
                    "ambient",
                    10_000L + i
            ));
        }

        assertFalse(tracker.utilitySignalRollup().isEmpty());
        assertTrue(tracker.highVolumeLowValueSignals().containsKey("PULSE@ON_WORLD_SCAN"));
    }
}
