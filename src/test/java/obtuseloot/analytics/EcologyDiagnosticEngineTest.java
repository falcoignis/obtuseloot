package obtuseloot.analytics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcologyDiagnosticEngineTest {
    private final EcologyDiagnosticEngine engine = new EcologyDiagnosticEngine();

    @Test
    void flagsFalseDivergenceWhenTurnoverIsHighButNoveltyIsWeak() {
        EcologyDiagnosticSnapshot snapshot = engine.diagnose(
                3.2D, 2.9D, 0.62D, 0.05D,
                0.61D, 0.66D, 0.66D,
                6, 8,
                List.of(0.02D, 0.04D, 0.05D),
                true,
                6);

        assertEquals(EcologyDiagnosticState.FALSE_DIVERGENCE, snapshot.state());
        assertTrue(snapshot.warningFlags().contains("false_divergence"));
    }

    @Test
    void detectsEmergentEcologyWhenNoveltyPersists() {
        EcologyDiagnosticSnapshot snapshot = engine.diagnose(
                4.1D, 3.8D, 0.34D, 0.33D,
                0.38D, 0.40D, 0.40D,
                5, 6,
                List.of(0.18D, 0.25D, 0.33D),
                false,
                1);

        assertEquals(EcologyDiagnosticState.EMERGENT_ECOLOGY, snapshot.state());
    }
}
