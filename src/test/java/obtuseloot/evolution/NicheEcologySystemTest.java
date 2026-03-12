package obtuseloot.evolution;

import obtuseloot.abilities.AbilityExecutionStatus;
import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityOutcomeType;
import obtuseloot.abilities.AbilityTrigger;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NicheEcologySystemTest {

    @Test
    void classifierAssignsDominantAndMultiNicheProfile() {
        EcosystemRoleClassifier classifier = new EcosystemRoleClassifier();
        ArtifactNicheProfile profile = classifier.classify(Map.of(
                "NAVIGATION_ANCHOR@ON_WORLD_SCAN", new MechanicUtilitySignal("NAVIGATION_ANCHOR@ON_WORLD_SCAN", 3.0D, 1.0D, 0.8D, 0.1D, 0.1D, 0.1D, 5L, 4L, 3.0D),
                "SENSE_PING@ON_STRUCTURE_SENSE", new MechanicUtilitySignal("SENSE_PING@ON_STRUCTURE_SENSE", 1.2D, 0.7D, 0.7D, 0.1D, 0.1D, 0.1D, 4L, 3L, 2.0D)
        ));

        assertNotNull(profile.dominantNiche());
        assertTrue(profile.niches().contains(MechanicNicheTag.NAVIGATION));
        assertTrue(profile.niches().contains(MechanicNicheTag.STRUCTURE_SENSING));
        assertTrue(profile.specialization().specializationScore() > 0.0D);
    }

    @Test
    void saturationTrackingUsesRuntimeTelemetry() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        ArtifactUsageProfile a = tracker.profileForSeed(1L);
        ArtifactUsageProfile b = tracker.profileForSeed(2L);

        a.recordUtilityOutcome(new UtilityOutcomeRecord("a", AbilityMechanic.PULSE, AbilityTrigger.ON_WORLD_SCAN,
                AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.WORLD_INTERACTION, true, true, 0.8D, 1.0D, "sim", 1L));
        a.recordUtilityOutcome(new UtilityOutcomeRecord("a", AbilityMechanic.NAVIGATION_ANCHOR, AbilityTrigger.ON_WORLD_SCAN,
                AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.INFORMATION, true, true, 0.9D, 1.0D, "sim", 2L));
        b.recordUtilityOutcome(new UtilityOutcomeRecord("b", AbilityMechanic.NAVIGATION_ANCHOR, AbilityTrigger.ON_WORLD_SCAN,
                AbilityExecutionStatus.NO_OP, AbilityOutcomeType.FLAVOR_ONLY, false, false, 0.4D, 1.3D, "sim", 3L));

        tracker.nichePopulationTracker().recordTelemetry(1L, a.utilitySignalsByMechanic());
        tracker.nichePopulationTracker().recordTelemetry(2L, b.utilitySignalsByMechanic());

        Map<MechanicNicheTag, NicheUtilityRollup> rollups = tracker.nichePopulationTracker().rollups();
        assertFalse(rollups.isEmpty());
        assertTrue(rollups.containsKey(MechanicNicheTag.NAVIGATION));
        assertTrue(rollups.get(MechanicNicheTag.NAVIGATION).activeArtifacts() >= 2);
    }

    @Test
    void ecologyPressureIsUtilityAwareNotRarityOnly() {
        EcosystemSaturationModel model = new EcosystemSaturationModel();
        NicheUtilityRollup crowdedWeak = new NicheUtilityRollup(MechanicNicheTag.NAVIGATION, 8, 40, 4, 1.5D, 30.0D);
        NicheUtilityRollup rareUseful = new NicheUtilityRollup(MechanicNicheTag.RITUAL_STRANGE_UTILITY, 1, 12, 9, 6.0D, 10.0D);
        Map<MechanicNicheTag, NicheUtilityRollup> all = Map.of(
                crowdedWeak.niche(), crowdedWeak,
                rareUseful.niche(), rareUseful
        );

        RolePressureMetrics weakPressure = model.pressureFor(crowdedWeak.niche(), crowdedWeak, all);
        RolePressureMetrics usefulPressure = model.pressureFor(rareUseful.niche(), rareUseful, all);

        assertTrue(weakPressure.netPressure() < usefulPressure.netPressure());
        assertTrue(usefulPressure.retentionBias() > weakPressure.retentionBias());
    }

    @Test
    void endToEndEcologySignalsShowWeakCrowdedSuppressionAndUsefulRareSupportAndSpecialization() {
        NichePopulationTracker tracker = new NichePopulationTracker();

        for (int i = 0; i < 6; i++) {
            tracker.recordTelemetry(10L + i, Map.of(
                    "NAVIGATION_ANCHOR@ON_WORLD_SCAN", new MechanicUtilitySignal("NAVIGATION_ANCHOR@ON_WORLD_SCAN", 0.4D, 0.05D, 0.4D, 0.4D, 0.4D, 0.3D, 12L, 1L, 10.0D)
            ));
        }

        tracker.recordTelemetry(99L, Map.of(
                "RITUAL_CHANNEL@ON_MEMORY_EVENT", new MechanicUtilitySignal("RITUAL_CHANNEL@ON_MEMORY_EVENT", 4.2D, 0.65D, 0.8D, 0.1D, 0.1D, 0.1D, 10L, 8L, 6.0D)
        ));

        RolePressureMetrics crowdedWeakPressure = tracker.pressureFor(10L);
        RolePressureMetrics rareUsefulPressure = tracker.pressureFor(99L);

        assertTrue(crowdedWeakPressure.netPressure() < rareUsefulPressure.netPressure());

        ArtifactNicheProfile rareProfile = tracker.nicheProfile(99L);
        assertNotNull(rareProfile.specialization());
        assertTrue(rareProfile.specialization().specializationScore() > 0.0D);
    }
}
