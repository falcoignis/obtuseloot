package obtuseloot.analytics.ecosystem;

import java.util.List;
import java.util.Map;

public record LongTermEvolutionReport(
        double ecosystemTurnover,
        Map<String, Long> lineageLifespanWindows,
        List<String> emergingNiches,
        double adaptationCycleStrength,
        String summary
) {
}
