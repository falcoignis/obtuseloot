package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.EcosystemTelemetryEvent;
import obtuseloot.telemetry.EcosystemTelemetryEventType;
import obtuseloot.telemetry.TelemetryRollupSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BranchSurvivalHalfLifeAnalyzer {

    public BranchSurvivalHalfLifeReport analyze(List<EcosystemTelemetryEvent> telemetryEvents,
                                                List<TelemetryRollupSnapshot> rollupHistory) {
        if (telemetryEvents == null || telemetryEvents.isEmpty() || rollupHistory == null || rollupHistory.isEmpty()) {
            return new BranchSurvivalHalfLifeReport(Double.NaN, 0, 0, List.of());
        }

        List<Long> windows = rollupHistory.stream().map(TelemetryRollupSnapshot::createdAtMs).sorted().toList();
        int lastWindow = windows.size();

        Map<Integer, List<String>> cohorts = new LinkedHashMap<>();
        Map<String, Integer> birthWindowByBranch = new LinkedHashMap<>();
        Map<String, Integer> collapseWindowByBranch = new LinkedHashMap<>();

        List<EcosystemTelemetryEvent> sorted = telemetryEvents.stream()
                .sorted(Comparator.comparingLong(EcosystemTelemetryEvent::timestampMs))
                .toList();

        for (EcosystemTelemetryEvent event : sorted) {
            String branchId = event.attributes().get("branch_id");
            if (branchId == null || branchId.isBlank()) {
                continue;
            }
            String lineageId = event.lineageId() == null ? "" : event.lineageId();
            String branchKey = lineageId + ":" + branchId;
            int window = resolveWindowIndex(event.timestampMs(), windows);
            if (event.type() == EcosystemTelemetryEventType.BRANCH_FORMATION) {
                if (birthWindowByBranch.putIfAbsent(branchKey, window) == null) {
                    cohorts.computeIfAbsent(window, ignored -> new ArrayList<>()).add(branchKey);
                }
            }
            if (event.type() == EcosystemTelemetryEventType.LINEAGE_UPDATE
                    && "branch-collapsed".equalsIgnoreCase(event.attributes().get("event"))) {
                collapseWindowByBranch.putIfAbsent(branchKey, window);
            }
        }

        List<CohortHalfLifeEstimate> estimates = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : cohorts.entrySet()) {
            int cohortWindow = entry.getKey();
            List<String> cohort = entry.getValue();
            int size = cohort.size();
            if (size == 0) {
                continue;
            }
            double threshold = size * 0.5D;
            Integer halfLifeAge = null;
            for (int window = cohortWindow; window <= lastWindow; window++) {
                int active = 0;
                for (String branch : cohort) {
                    Integer collapseWindow = collapseWindowByBranch.get(branch);
                    if (collapseWindow == null || collapseWindow > window) {
                        active++;
                    }
                }
                if (active <= threshold) {
                    halfLifeAge = (window - cohortWindow) + 1;
                    break;
                }
            }
            boolean censored = halfLifeAge == null;
            double observed = censored ? (lastWindow - cohortWindow) + 1 : halfLifeAge;
            estimates.add(new CohortHalfLifeEstimate(cohortWindow, size, observed, censored));
        }

        if (estimates.isEmpty()) {
            return new BranchSurvivalHalfLifeReport(Double.NaN, 0, 0, List.of());
        }
        double aggregate = estimates.stream().mapToDouble(CohortHalfLifeEstimate::halfLifeWindows).average().orElse(Double.NaN);
        int censoredCount = (int) estimates.stream().filter(CohortHalfLifeEstimate::censored).count();
        return new BranchSurvivalHalfLifeReport(aggregate, estimates.size(), censoredCount, List.copyOf(estimates));
    }

    private int resolveWindowIndex(long timestampMs, List<Long> windows) {
        for (int i = 0; i < windows.size(); i++) {
            if (timestampMs <= windows.get(i)) {
                return i + 1;
            }
        }
        return windows.size();
    }
}

