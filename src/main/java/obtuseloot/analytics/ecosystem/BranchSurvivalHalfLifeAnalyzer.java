package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.EcosystemTelemetryEvent;
import obtuseloot.telemetry.EcosystemTelemetryEventType;
import obtuseloot.telemetry.TelemetryRollupSnapshot;
import obtuseloot.lineage.BranchLifecycleState;

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
        Map<String, Integer> firstInactiveWindowByBranch = new LinkedHashMap<>();

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
            if (event.type() == EcosystemTelemetryEventType.LINEAGE_UPDATE) {
                if ("branch-collapsed".equalsIgnoreCase(event.attributes().get("event"))) {
                    collapseWindowByBranch.putIfAbsent(branchKey, window);
                }
                BranchLifecycleState state = parseLifecycleState(event.attributes().get("lifecycle_state"));
                if (state != null && state != BranchLifecycleState.STABLE) {
                    firstInactiveWindowByBranch.putIfAbsent(branchKey, window);
                }
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
            List<Integer> activeByWindow = new ArrayList<>();
            List<Integer> inactiveOrDeadByWindow = new ArrayList<>();
            for (int window = cohortWindow; window <= lastWindow; window++) {
                int active = 0;
                int inactiveOrDead = 0;
                for (String branch : cohort) {
                    Integer collapseWindow = collapseWindowByBranch.get(branch);
                    Integer inactiveWindow = firstInactiveWindowByBranch.get(branch);
                    boolean collapsed = collapseWindow != null && collapseWindow <= window;
                    boolean inactive = inactiveWindow != null && inactiveWindow <= window;
                    if (!collapsed && !inactive) {
                        active++;
                    } else {
                        inactiveOrDead++;
                    }
                }
                activeByWindow.add(active);
                inactiveOrDeadByWindow.add(inactiveOrDead);
                if (active <= threshold) {
                    halfLifeAge = (window - cohortWindow) + 1;
                    break;
                }
            }
            boolean censored = halfLifeAge == null;
            double observed = censored ? Double.NaN : halfLifeAge;
            estimates.add(new CohortHalfLifeEstimate(cohortWindow, size, observed, censored,
                    List.copyOf(activeByWindow), List.copyOf(inactiveOrDeadByWindow)));
        }

        if (estimates.isEmpty()) {
            return new BranchSurvivalHalfLifeReport(Double.NaN, 0, 0, List.of());
        }
        double aggregate = estimates.stream()
                .filter(e -> !e.censored())
                .mapToDouble(CohortHalfLifeEstimate::halfLifeWindows)
                .average()
                .orElse(Double.NaN);
        int censoredCount = (int) estimates.stream().filter(CohortHalfLifeEstimate::censored).count();
        return new BranchSurvivalHalfLifeReport(aggregate, estimates.size(), censoredCount, List.copyOf(estimates));
    }

    private BranchLifecycleState parseLifecycleState(String rawState) {
        if (rawState == null || rawState.isBlank()) {
            return null;
        }
        try {
            return BranchLifecycleState.valueOf(rawState.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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
