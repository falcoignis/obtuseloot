package obtuseloot.analytics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EcologyAlertEngine {

    public enum Severity { INFO, WARNING, ERROR }

    public record Alert(String code, Severity severity, String message) {}

    public record AlertThresholds(boolean enabled,
                                  boolean failOnError,
                                  double minEND,
                                  double maxTNT,
                                  double minNSER,
                                  int minPNNC,
                                  boolean warnIfFalseDivergence,
                                  boolean failIfNoveltyRegresses) {
        public static AlertThresholds defaults() {
            return new AlertThresholds(true, true, 2.5D, 0.60D, 0.05D, 1, true, false);
        }
    }

    public record AlertResult(AlertThresholds thresholds,
                              List<Alert> alerts,
                              String regressionGate,
                              boolean shouldFail) {
    }

    public AlertResult evaluate(EcologyDiagnosticSnapshot diagnostic,
                                PersistentNovelNicheAnalyzer.PnncResult pnnc,
                                AlertThresholds thresholds,
                                Integer priorBaselinePnnc) {
        if (!thresholds.enabled()) {
            return new AlertResult(thresholds, List.of(), "DISABLED", false);
        }
        List<Alert> alerts = new ArrayList<>();

        if (thresholds.warnIfFalseDivergence() && diagnostic.state() == EcologyDiagnosticState.FALSE_DIVERGENCE && pnnc.currentPnnc() == 0) {
            alerts.add(new Alert("FALSE_DIVERGENCE", Severity.WARNING,
                    "END/TNT activity without durable novelty: NSER=" + diagnostic.latestNser() + ", PNNC=0."));
        }
        if (pnnc.currentPnnc() == 0 && diagnostic.latestTnt() >= 0.20D) {
            alerts.add(new Alert("BOUNDED_RESHUFFLING", Severity.WARNING,
                    "Turnover exists but PNNC remains zero over the evaluated window."));
        }
        if (diagnostic.endArtifacts() >= thresholds.minEND() && diagnostic.latestTnt() <= thresholds.maxTNT()
                && diagnostic.latestNser() >= thresholds.minNSER() && pnnc.currentPnnc() >= thresholds.minPNNC()) {
            alerts.add(new Alert("EMERGENT_ECOLOGY", Severity.INFO,
                    "END/TNT/NSER/PNNC jointly indicate durable ecological expansion."));
        }
        if (priorBaselinePnnc != null && priorBaselinePnnc > 0 && pnnc.currentPnnc() < thresholds.minPNNC()) {
            Severity severity = thresholds.failIfNoveltyRegresses() ? Severity.ERROR : Severity.WARNING;
            alerts.add(new Alert("NOVELTY_REGRESSION", severity,
                    "Baseline PNNC=" + priorBaselinePnnc + " regressed to PNNC=" + pnnc.currentPnnc() + "."));
        }

        boolean hasError = alerts.stream().anyMatch(a -> a.severity() == Severity.ERROR);
        String gate = hasError ? "FAIL" : alerts.stream().anyMatch(a -> a.severity() == Severity.WARNING) ? "WARNING" : "PASS";
        boolean shouldFail = hasError && thresholds.failOnError();
        return new AlertResult(thresholds, List.copyOf(alerts), gate, shouldFail);
    }

    public Map<String, Object> asJson(EcologyDiagnosticSnapshot diagnostic,
                                      PersistentNovelNicheAnalyzer.PnncResult pnnc,
                                      AlertResult result) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ecologyState", diagnostic.state().name());
        out.put("END", diagnostic.endArtifacts());
        out.put("TNT", diagnostic.latestTnt());
        out.put("NSER", diagnostic.latestNser());
        out.put("PNNC", pnnc.currentPnnc());
        out.put("regressionGate", result.regressionGate());
        out.put("shouldFail", result.shouldFail());

        List<Map<String, Object>> alertRows = new ArrayList<>();
        for (Alert alert : result.alerts()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", alert.code());
            row.put("severity", alert.severity().name());
            row.put("message", alert.message());
            alertRows.add(row);
        }
        out.put("alerts", alertRows);
        return out;
    }
}
