package obtuseloot.ecosystem;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcosystemBiasCalculatorTest {
    @Test
    void ecosystemBiasesAreWithinPlusMinusFifteenPercent() {
        EcosystemBiasCalculator calculator = new EcosystemBiasCalculator();
        WorldEcosystemProfile profile = new WorldEcosystemProfile(1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D);

        Map<String, Double> biases = calculator.calculate(profile);
        assertEquals(4, biases.size());
        for (double bias : biases.values()) {
            assertTrue(bias <= 0.15D);
            assertTrue(bias >= -0.15D);
        }
    }
}
