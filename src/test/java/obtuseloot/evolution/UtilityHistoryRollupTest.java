package obtuseloot.evolution;

import obtuseloot.abilities.AbilityExecutionStatus;
import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityOutcomeType;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.artifacts.Artifact;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UtilityHistoryRollupTest {

    @Test
    void utilityHistoryRollupPersistsAcrossHydrationCycle() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactSeed(42L);

        ArtifactUsageProfile profile = tracker.profileFor(artifact);
        for (int i = 0; i < 6; i++) {
            profile.recordUtilityOutcome(new UtilityOutcomeRecord(
                    "a",
                    AbilityMechanic.HARVEST_RELAY,
                    AbilityTrigger.ON_BLOCK_HARVEST,
                    AbilityExecutionStatus.SUCCESS,
                    AbilityOutcomeType.CROP_REPLANT,
                    true,
                    true,
                    1.2D,
                    0.6D,
                    "intent",
                    100L + i));
        }

        String persisted = tracker.utilityHistoryFor(artifact).encode();
        artifact.setLastUtilityHistory(persisted);

        ArtifactUsageTracker reloadedTracker = new ArtifactUsageTracker();
        reloadedTracker.hydrateFromArtifact(artifact);
        ArtifactUsageProfile reloaded = reloadedTracker.profileFor(artifact);

        assertTrue(reloaded.validatedUtilityScore() > 0.0D);
        assertTrue(reloaded.utilityDensity() > 0.0D);
        assertEquals(profile.meaningfulOutcomeRate(), reloaded.meaningfulOutcomeRate(), 0.02D);
    }

    @Test
    void preferredMutationSignalsUseHighUtilityHistoryWhenAvailable() {
        ArtifactUsageProfile profile = new ArtifactUsageProfile();
        for (int i = 0; i < 7; i++) {
            profile.recordUtilityOutcome(new UtilityOutcomeRecord(
                    "b",
                    AbilityMechanic.NAVIGATION_ANCHOR,
                    AbilityTrigger.ON_WORLD_SCAN,
                    AbilityExecutionStatus.SUCCESS,
                    AbilityOutcomeType.NAVIGATION_HINT,
                    true,
                    true,
                    1.1D,
                    0.7D,
                    "intent",
                    200L + i));
        }
        for (int i = 0; i < 12; i++) {
            profile.recordUtilityOutcome(new UtilityOutcomeRecord(
                    "c",
                    AbilityMechanic.PULSE,
                    AbilityTrigger.ON_WORLD_SCAN,
                    AbilityExecutionStatus.NO_OP,
                    AbilityOutcomeType.FLAVOR_ONLY,
                    false,
                    false,
                    0.6D,
                    1.5D,
                    "ambient",
                    300L + i));
        }

        UtilityHistoryRollup rollup = UtilityHistoryRollup.fromProfile(profile);
        assertEquals(AbilityTrigger.ON_WORLD_SCAN, rollup.preferredTrigger(AbilityTrigger.ON_HIT));
        assertEquals(AbilityMechanic.NAVIGATION_ANCHOR, rollup.preferredMechanic(AbilityMechanic.PULSE));
    }

    @Test
    void sparseHistoryFallsBackToColdStartBehavior() {
        UtilityHistoryRollup rollup = UtilityHistoryRollup.empty();
        assertFalse(rollup.hasUtilityHistory());
        assertEquals(AbilityTrigger.ON_HIT, rollup.preferredTrigger(AbilityTrigger.ON_HIT));
        assertEquals(AbilityMechanic.PULSE, rollup.preferredMechanic(AbilityMechanic.PULSE));
    }
}
