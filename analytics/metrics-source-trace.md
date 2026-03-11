# Metrics Source Trace

- Authoritative source: `analytics/ecology-truth-snapshot.json` generated once per world-lab run from a single in-memory snapshot.
- END/TNT/NSER/PNNC, dominant niche share, niche count, species count, and diagnostic state are all serialized from that snapshot.

## Compute points
- END/TNT: `EcosystemHealthGaugeAnalyzer` in `writeEcosystemHealthGauge(...)`.
- NSER: `NovelStrategyEmergenceAnalyzer` in `writeNovelStrategyEmergenceReports(...)`, then copied into truth snapshot.
- PNNC: `PersistentNovelNicheAnalyzer` in `writePersistentNovelNicheReports(...)`, then copied into truth snapshot.
- Diagnostic state: `EcologyDiagnosticEngine` in `writeEcosystemHealthGauge(...)` using the same latest END/TNT/NSER/PNNC values.

## Cache/consumer alignment
- Cached/serialized at: `analytics/ecology-truth-snapshot.json`.
- Consumed by: diagnostic report, ecosystem health gauge, persistent novelty reports, impact reviews, open-endedness reconciliation, dashboard service.
- Prior inconsistency root cause: report-specific fallbacks and stale markdown text were not bound to one run-level snapshot.
