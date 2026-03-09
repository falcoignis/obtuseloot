package obtuseloot.ecosystem;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcosystemDiversityControllerTest {
    @Test
    void rarityBoostsAndDominanceSuppressesWithoutCollapse() {
        EcosystemDiversityController controller = new EcosystemDiversityController();
        Map<String, Double> adjustments = controller.computeAdjustments(Map.of(
                "chaos", 80,
                "precision", 10,
                "survival", 5,
                "mobility", 5
        ));

        assertTrue(adjustments.get("precision") > 0.0D);
        assertTrue(adjustments.get("survival") > adjustments.get("precision"));
        assertTrue(adjustments.get("chaos") < adjustments.get("precision"));
    }

    @Test
    void effectiveFitnessUsesNichePopulation() {
        EcosystemDiversityController controller = new EcosystemDiversityController();
        assertEquals(4.0D, controller.effectiveFitness(20.0D, 5), 1.0E-9D);
        assertEquals(20.0D, controller.effectiveFitness(20.0D, 0), 1.0E-9D);
    }
}
