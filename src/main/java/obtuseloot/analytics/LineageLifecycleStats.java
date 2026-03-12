package obtuseloot.analytics;

import java.util.Map;

public record LineageLifecycleStats(
        Map<String, Integer> descendantsObserved,
        Map<String, Integer> branchBirths,
        Map<String, Integer> branchCollapses,
        Map<String, Integer> branchSurvivors,
        Map<String, String> lineageState
) {
}

