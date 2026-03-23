package obtuseloot.ecosystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 8.1A — Guard Stacking Cap.
 *
 * Verifies that the combined category × template suppression multiplier is clamped
 * to {@link ProductionSafetyConfig#minCombinedSuppression()} when both guards fire.
 */
class ProductionSafetyGuardStackingTest {

    private static final Logger NO_OP_LOGGER = Logger.getLogger("test-noop");

    private ProductionSafetyConfig config;
    private ProductionSafetyGuards guards;

    @BeforeEach
    void setUp() {
        config = ProductionSafetyConfig.defaults();
        guards = new ProductionSafetyGuards(config, NO_OP_LOGGER);
    }

    /** Saturate both windows so both guards are active. */
    private void saturateBothGuards() {
        Map<String, Double> dominantCategory = Map.of("chaos", 0.80, "precision", 0.20);
        Map<String, Double> dominantTemplate = Map.of("on_kill", 0.70, "on_hit", 0.30);
        for (int i = 0; i < config.rollingWindowSize(); i++) {
            guards.recordCategoryDistribution(dominantCategory);
            guards.recordTemplateDistribution(dominantTemplate);
        }
    }

    @Test
    void combinedMultiplierIsFlooredWhenBothGuardsFire() {
        saturateBothGuards();

        double catMult = guards.categoryGuardMultiplier("chaos");
        double tmplMult = guards.templateGuardMultiplier("on_kill");

        // Verify both guards are active
        assertTrue(catMult < 1.0, "Category guard must be active");
        assertTrue(tmplMult < 1.0, "Template guard must be active");

        // Raw product would be below the floor (0.85 × 0.90 = 0.765 < 0.75 is false here,
        // but the floor must still be respected regardless)
        double rawProduct = catMult * tmplMult;
        double combined = guards.combinedGuardMultiplier("chaos", "on_kill");

        assertEquals(Math.max(config.minCombinedSuppression(), rawProduct), combined, 1e-9,
                "combinedGuardMultiplier must equal max(minCombinedSuppression, raw product)");
        assertTrue(combined >= config.minCombinedSuppression(),
                "Combined multiplier must not fall below minCombinedSuppression floor");
    }

    @Test
    void combinedMultiplierFloorIsEnforcedAtExactDefaultValues() {
        // With default factors 0.85 × 0.90 = 0.765, floor is 0.75 → result is 0.765
        // (floor doesn't clamp this particular pair, but must be >= 0.75)
        saturateBothGuards();
        double combined = guards.combinedGuardMultiplier("chaos", "on_kill");
        assertTrue(combined >= 0.75,
                "Combined multiplier must be >= 0.75 (default floor) when both guards fire");
    }

    @Test
    void combinedMultiplierIsOneWhenNoGuardsFire() {
        // No observations — neither guard fires
        double combined = guards.combinedGuardMultiplier("chaos", "on_kill");
        assertEquals(1.0, combined, 1e-9,
                "Combined multiplier must be 1.0 when no guards are active");
    }

    @Test
    void combinedMultiplierWithCustomLowFloorIsRespected() {
        // Create a config where floor is higher than the raw product to force clamping
        ProductionSafetyConfig strictConfig = new ProductionSafetyConfig(
                0.65, 0.50,   // category suppression = 0.50
                0.55, 0.50,   // template suppression = 0.50
                2, 10, false, 500, false, 1200,
                0.80,         // minCombinedSuppression = 0.80 (floor above 0.50 × 0.50 = 0.25)
                3000L
        );
        ProductionSafetyGuards strictGuards = new ProductionSafetyGuards(strictConfig, NO_OP_LOGGER);

        Map<String, Double> dominant = Map.of("chaos", 0.90, "precision", 0.10);
        Map<String, Double> dominantTmpl = Map.of("on_kill", 0.90, "on_hit", 0.10);
        for (int i = 0; i < strictConfig.rollingWindowSize(); i++) {
            strictGuards.recordCategoryDistribution(dominant);
            strictGuards.recordTemplateDistribution(dominantTmpl);
        }

        double combined = strictGuards.combinedGuardMultiplier("chaos", "on_kill");
        // Raw would be 0.50 × 0.50 = 0.25, floor is 0.80
        assertEquals(0.80, combined, 1e-9,
                "Floor must clamp combined multiplier up to minCombinedSuppression");
    }

    @Test
    void combinedMultiplierOnlyOneCategoryGuardFires() {
        // Only category guard fires — template window empty
        Map<String, Double> dominant = Map.of("chaos", 0.80, "precision", 0.20);
        for (int i = 0; i < config.rollingWindowSize(); i++) {
            guards.recordCategoryDistribution(dominant);
        }

        double combined = guards.combinedGuardMultiplier("chaos", "on_kill");
        double expected = Math.max(config.minCombinedSuppression(),
                config.categorySuppressionFactor() * 1.0);
        assertEquals(expected, combined, 1e-9,
                "Combined multiplier with only category guard active must equal clamped category factor");
    }
}
