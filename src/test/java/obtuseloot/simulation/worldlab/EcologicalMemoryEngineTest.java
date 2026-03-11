package obtuseloot.simulation.worldlab;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EcologicalMemoryEngineTest {

    @Test
    void activatesOnlyAfterPersistentDominanceWindow() {
        EcologicalMemoryEngine engine = new EcologicalMemoryEngine();
        for (int i = 0; i < 3; i++) {
            engine.observeSeason(
                    Map.of("a", 90, "b", 10),
                    Map.of("n1", 85, "n2", 15),
                    Map.of("s1", 88, "s2", 12),
                    Map.of("t1", 92, "t2", 8),
                    Map.of("m1", 91, "m2", 9),
                    Map.of("env1", 89, "env2", 11));
        }
        assertFalse(engine.feedback().active());

        engine.observeSeason(
                Map.of("a", 90, "b", 10),
                Map.of("n1", 85, "n2", 15),
                Map.of("s1", 88, "s2", 12),
                Map.of("t1", 92, "t2", 8),
                Map.of("m1", 91, "m2", 9),
                Map.of("env1", 89, "env2", 11));

        assertTrue(engine.feedback().active());
        assertTrue(engine.feedback().pressure() > 0.0D);
        assertTrue(engine.feedback().modifier() <= 1.0D);
        assertTrue(engine.feedback().modifier() >= 0.90D);
    }
}
