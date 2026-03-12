# Phase 6.5 Operational Analytics Forensic Audit (2026-03-12)

## Scope
Code-path verification of the Phase 6.5 analytics layer as implemented under `src/main/java/obtuseloot/analytics/ecosystem` and related telemetry/simulation modules.

## Verdict
**PARTIAL**.

Phase 6.5 introduces a real orchestration code path (`EcosystemAnalyticsRunner -> AnalyticsJobOrchestrator -> EcosystemAnalyticsOrchestrator`) and governance/export primitives, but the workflow is not yet wired to production CLI/scheduler/runtime entry points. The pipeline is currently exercised by tests, not by shipped operational commands/jobs.

## Execution path traced
1. `EcosystemAnalyticsRunner.run(EcosystemAnalysisJob)` prepares context via `AnalyticsJobOrchestrator.prepare(...)`.
2. `AnalyticsJobOrchestrator` loads telemetry from `EcosystemHistoryArchive` and rollups from `TelemetryRollupSnapshotStore` files plus optional harness directory input.
3. `EcosystemAnalyticsOrchestrator.analyze(window, history)` runs:
   - `EcosystemTrendAnalyzer`
   - `LineageSuccessAnalyzer`
   - `EcosystemAnomalyDetector` (with history-aware baselines)
   - `EcosystemTuningRecommender`
   - `LongTermEvolutionAnalyzer`
4. Runner persists a report and recommendation record to `recommendation-history.log`.
5. Governance decision + export are separate explicit actions through `RecommendationHistoryStore.setDecision(...)` and `TuningProfileExport.exportAccepted(...)`.

## What is operational today
- End-to-end orchestration classes exist and run in tests.
- Baseline-aware anomaly thresholds use historical percentiles (`diversity_p50`, `turnover_p75`) when history size >= 3.
- Recommendation lifecycle states and persistent history store exist.
- Exported tuning profiles map to runtime registry keys and include metadata.
- Analytics modules do not mutate runtime registry/plugin state automatically.
- Harness output support is partial (metadata + root rollup file expectation), but path conventions are inconsistent with world harness output layout.

## Gaps / limitations
- No production entry-point wiring found for `EcosystemAnalyticsRunner` (no command, scheduler, or startup hook invoking it).
- No built-in scheduled batch job for repeated historical analyses.
- Harness path mismatch: orchestrator expects `harnessDir/rollup-snapshot.properties`, while world harness writes telemetry snapshot under `output/telemetry/rollup-snapshot.properties`.
- Harness `telemetry-events.log` (stringified objects) is not consumed by `EcosystemHistoryArchive` parser format.
- Time-windowing is explicit but coarse; there is no lineage/niche trend-by-window decomposition beyond first-vs-last style calculations.
- Governance test coverage exists but does not verify all lifecycle transitions (`REJECTED`, `SUPERSEDED`, `APPLIED`) or audit-note semantics deeply.

## Recommendation
To reach "FULLY IMPLEMENTED operational workflow", wire `EcosystemAnalyticsRunner` into:
- an offline CLI entry point,
- a scheduled batch trigger,
- a harness-compatible loader that resolves telemetry files from actual harness output structure.
