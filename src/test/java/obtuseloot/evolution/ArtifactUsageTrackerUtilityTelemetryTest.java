package obtuseloot.evolution;

import obtuseloot.abilities.AbilityExecutionStatus;
import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityOutcomeType;
import obtuseloot.abilities.AbilityTrigger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactUsageTrackerUtilityTelemetryTest {

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
