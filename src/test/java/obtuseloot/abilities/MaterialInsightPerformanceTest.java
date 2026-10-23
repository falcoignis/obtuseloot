package obtuseloot.abilities;

import obtuseloot.abilities.genome.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialInsightPerformanceTest {
    @Test void catalogExpressesInformationDepthAndCommittedRecoveryFromPrecision() {
        var model = new AbilityRegistry().registrationById("precision.material_insight").performanceModel();
        var low = model.express(genome(0)); var high = model.express(genome(1));
        assertEquals(2, low.output(4)); assertEquals(6, high.output(4));
        assertEquals(30, low.recoveryTicks(60)); assertEquals(90, high.recoveryTicks(60));
    }
    static ArtifactGenome genome(double precision) {
        var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        for (var trait : GenomeTrait.values()) traits.put(trait, .5);
        traits.put(GenomeTrait.PRECISION_AFFINITY, precision);
        return new ArtifactGenome(19, traits);
    }
}
