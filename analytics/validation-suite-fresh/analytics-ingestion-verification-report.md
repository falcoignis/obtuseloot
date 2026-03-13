# Analytics Ingestion Verification Report

## SECTION 1: ANALYTICS ENTRY PATH USED
- Entry point executed: `obtuseloot.analytics.ecosystem.AnalyticsCliMain` using `analyze`.
- Command pattern used:
  - `java -cp target/classes obtuseloot.analytics.ecosystem.AnalyticsCliMain analyze --dataset analytics/validation-suite/runs/<scenario> --output analytics/validation-suite-fresh/live-analysis-attempt/<scenario> --job-id fresh-<scenario>`

## SECTION 2: DATASETS INGESTED
Datasets inspected under `analytics/validation-suite/runs/*` (as fresh harness-equivalent artifacts available in-repo):
- `explorer-heavy`
- `ritualist-heavy`
- `gatherer-heavy`
- `mixed`
- `random-baseline`

Artifact presence checks:
- telemetry archive (`telemetry/ecosystem-events.log`): **missing for all scenarios**.
- rollup snapshots JSON (`rollup-snapshots.json`): present for all scenarios.
- rollup snapshot properties (`telemetry/rollup-snapshot.properties`): present for all scenarios.
- scenario metadata (`scenario-metadata.properties`): present for all scenarios.
- minimal world summary (`world-sim-data.json`): present for all scenarios.

## SECTION 3: PER-SCENARIO ANALYTICS COMPLETION STATUS
Fresh CLI execution (`live-analysis-attempt`) status:
- `explorer-heavy`: **FAILED** (missing telemetry archive contract requirement).
- `ritualist-heavy`: **FAILED** (missing telemetry archive contract requirement).
- `gatherer-heavy`: **FAILED** (missing telemetry archive contract requirement).
- `mixed`: **FAILED** (missing telemetry archive contract requirement).
- `random-baseline`: **FAILED** (missing telemetry archive contract requirement).

Previously generated analytics artifacts (`analytics/validation-suite/analysis/*`) status:
- All five scenarios have complete output bundles present:
  - analysis report
  - job record
  - output manifest
  - run metadata
  - recommendation history

## SECTION 4: TELEMETRY / ROLLUP INGESTION COHERENCE
- Telemetry ingestion (fresh rerun): **NOT COHERENT**; blocked by missing telemetry archive in every scenario dataset.
- Rollup snapshot ingestion readiness:
  - Coherent for rollup files themselves (JSON + `.properties` present).
  - End-to-end ingestion still blocked by telemetry archive contract failure.
- Contract expectation source:
  - `TelemetryDatasetContract` and `HarnessOutputAdapter` require telemetry archive presence.

## SECTION 5: ANOMALY / RECOMMENDATION OUTPUTS
From existing reports (`analytics/validation-suite/analysis/*/validation-*-analysis-report.txt`):
- Anomaly detection executed previously:
  - `runaway_lineages=[]`
  - `niche_collapse=[]`
  - `severity=0.000`
- Recommendation generation executed previously:
  - `recommendation_id` populated in all five scenarios.
  - `decision=PROPOSED` in all five scenarios.

## SECTION 6: HISTORICAL-DEPTH / WINDOW ANALYSIS STATUS
From existing reports:
- `rollups_loaded=1` for all five scenarios.
- `window_size=1` for all five scenarios.
- `long_term_summary=Insufficient historical rollups for long-term analysis.` for all five scenarios.

Interpretation:
- Long-term depth is shallow and below the threshold for multi-window historical analysis.

## SECTION 7: ANALYTICS FAILURES OR CONTRACT GAPS
Detected gaps:
1. Missing telemetry archive (`telemetry/ecosystem-events.log`) in all scenario run datasets.
2. Fresh analytics pipeline cannot ingest harness outputs due to contract enforcement.
3. Historical/window depth insufficient (`rollups_loaded=1`) for long-term analyzer depth.

Primary failure signature from CLI logs:
- `IllegalArgumentException: Harness dataset missing telemetry archive at telemetry/ecosystem-events.log`.

## SECTION 8: ANALYTICS READINESS VERDICT
**PARTIALLY COHERENT**

Rationale:
- Historical analytics outputs are present and internally structured for all scenarios.
- Fresh ingestion/re-execution is currently blocked by missing telemetry archives, so readiness is not fully coherent.
