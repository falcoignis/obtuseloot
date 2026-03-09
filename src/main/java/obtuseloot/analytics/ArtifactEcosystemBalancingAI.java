package obtuseloot.analytics;

import java.util.Map;

public class ArtifactEcosystemBalancingAI {
    private final EcosystemBalanceAnalyzer analyzer = new EcosystemBalanceAnalyzer();

    public EcosystemHealthReport evaluate(Map<String, Integer> families,
                                          Map<String, Integer> branches,
                                          Map<String, Integer> mutations,
                                          Map<String, Integer> triggers,
                                          Map<String, Integer> mechanics,
                                          Map<String, Integer> memories) {
        return analyzer.analyze(families, branches, mutations, triggers, mechanics, memories);
    }
}
