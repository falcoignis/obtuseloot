package obtuseloot.evolution;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EcosystemSaturationModelTest {

    @Test
    void computesIndependentSaturationAndSpecializationPressuresForBifurcation() {
        EcosystemSaturationModel model = new EcosystemSaturationModel();
        NicheUtilityRollup navigation = new NicheUtilityRollup(
                MechanicNicheTag.NAVIGATION,
                36,
                100,
                18,
                8.0D,
                20.0D);

        Map<MechanicNicheTag, NicheUtilityRollup> rollups = Map.of(
                MechanicNicheTag.NAVIGATION, navigation,
                MechanicNicheTag.STRUCTURE_SENSING, new NicheUtilityRollup(MechanicNicheTag.STRUCTURE_SENSING, 39, 100, 12, 10.0D, 20.0D),
                MechanicNicheTag.ENVIRONMENTAL_SENSING, new NicheUtilityRollup(MechanicNicheTag.ENVIRONMENTAL_SENSING, 25, 100, 10, 6.0D, 20.0D)
        );

        RolePressureMetrics pressure = model.pressureFor(MechanicNicheTag.NAVIGATION, navigation, rollups);

        assertTrue(pressure.saturationPenalty() >= 0.15D, "saturationPenalty should reflect crowding share above threshold");
        assertTrue(pressure.specializationPressure() >= 0.10D, "specializationPressure should reflect differentiation score above threshold");
        assertTrue(pressure.saturationPenalty() > 0.0D, "saturationPenalty should be active");
        assertTrue(pressure.specializationPressure() > 0.0D, "specializationPressure should be active");

        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 2);

        Optional<NicheBifurcation> firstWindow = registry.evaluateBifurcation(
                MechanicNicheTag.NAVIGATION.name(),
                pressure.saturationPenalty(),
                0.60D,
                0.36D,
                navigation.activeArtifacts(),
                1_000L);
        Optional<NicheBifurcation> secondWindow = registry.evaluateBifurcation(
                MechanicNicheTag.NAVIGATION.name(),
                pressure.saturationPenalty(),
                0.60D,
                0.36D,
                navigation.activeArtifacts(),
                1_001L);

        assertTrue(firstWindow.isEmpty(), "first sustained-pressure window should accumulate without bifurcation");
        assertTrue(secondWindow.isPresent(), "second sustained-pressure window should trigger bifurcation");
    }
}
