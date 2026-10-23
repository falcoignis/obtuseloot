package obtuseloot.ecology;

import org.junit.jupiter.api.Test;

/** Same seeds, observations and lifecycle as the frozen baseline; only habitat capture differs. */
class HabitatCompetitionBenchmarkTest {
    @Test void compareConditionalCompetitionAcrossTheSameEightSeedPairs() throws Exception {
        new LocalEcosystemHealthBaselineTest().compare(true);
    }
}
