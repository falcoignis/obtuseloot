package obtuseloot.ecosystem;

import obtuseloot.abilities.genome.GenomeTrait;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentPressureEngineTest {
    @Test
    void initializesWithKnownEventAndMultipliers() {
        EnvironmentPressureEngine engine = new EnvironmentPressureEngine(9L);
        assertNotNull(engine.currentEvent());
        assertTrue(engine.currentEvent().remainingSeasons() >= 1);
        assertTrue(engine.currentEvent().remainingSeasons() <= 5);
        assertTrue(engine.currentModifiers().containsKey(GenomeTrait.PRECISION_AFFINITY));
    }

    @Test
    void advancesSeasonsAndRotatesWhenExpired() {
        EnvironmentPressureEngine engine = new EnvironmentPressureEngine(2L);
        String initialName = engine.currentEvent().name();
        int initialDuration = engine.currentEvent().remainingSeasons();

        for (int i = 0; i < initialDuration; i++) {
            engine.advanceSeason();
        }

        assertEquals(initialDuration, engine.elapsedSeasons());
        assertNotNull(engine.currentEvent().name());
        assertTrue(engine.currentEvent().remainingSeasons() >= 1);
        // rotation may pick same event name; verify modifiers are still accessible.
        assertTrue(engine.multiplierFor(GenomeTrait.MOBILITY_AFFINITY) > 0.0D);
        assertNotNull(initialName);
    }
}
