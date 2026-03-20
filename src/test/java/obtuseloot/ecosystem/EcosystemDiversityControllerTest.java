package obtuseloot.ecosystem;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcosystemDiversityControllerTest {
    @Test
    void dominantArchetypeIsSuppressedAndNonDominantIsNeutral() {
        EcosystemDiversityController controller = new EcosystemDiversityController();
        Map<String, Double> adjustments = controller.computeAdjustments(Map.of(
                "chaos", 80,
                "precision", 10,
                "survival", 5,
                "mobility", 5
        ));

        assertTrue(adjustments.get("chaos") < 0.0D);
        assertEquals(0.0D, adjustments.get("precision"), 1.0E-9D);
        assertEquals(0.0D, adjustments.get("survival"), 1.0E-9D);
        assertEquals(0.0D, adjustments.get("mobility"), 1.0E-9D);
    }

    @Test
    void effectiveFitnessUsesNichePopulation() {
        EcosystemDiversityController controller = new EcosystemDiversityController();
        assertEquals(4.0D, controller.effectiveFitness(20.0D, 5), 1.0E-9D);
        assertEquals(20.0D, controller.effectiveFitness(20.0D, 0), 1.0E-9D);
    }
}
