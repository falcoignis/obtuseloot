SECTION 1: ANALYTICS ENTRY PATH USED
- CLI entry point: `obtuseloot.analytics.ecosystem.AnalyticsCliMain` (command used: `analyze`).
- Execution command template: `java -cp target/classes obtuseloot.analytics.ecosystem.AnalyticsCliMain analyze --dataset <scenario-run-dir> --output <scenario-output-dir> --job-id <id>`.

SECTION 2: DATASETS INGESTED
- `explorer-heavy`: telemetry archive=False, rollup snapshots json=True, rollup snapshot properties=True, scenario metadata=True, minimal world summary(world-sim-data)=True.
- `ritualist-heavy`: telemetry archive=False, rollup snapshots json=True, rollup snapshot properties=True, scenario metadata=True, minimal world summary(world-sim-data)=True.
- `gatherer-heavy`: telemetry archive=False, rollup snapshots json=True, rollup snapshot properties=True, scenario metadata=True, minimal world summary(world-sim-data)=True.
- `mixed`: telemetry archive=False, rollup snapshots json=True, rollup snapshot properties=True, scenario metadata=True, minimal world summary(world-sim-data)=True.
- `random-baseline`: telemetry archive=False, rollup snapshots json=True, rollup snapshot properties=True, scenario metadata=True, minimal world summary(world-sim-data)=True.

SECTION 3: PER-SCENARIO ANALYTICS COMPLETION STATUS
- `explorer-heavy` existing committed analytics artifacts: analysis report=yes, job record=yes, output manifest=yes, run metadata=yes, recommendation history=yes.
  - Fresh pipeline rerun status: FAILED (`Harness dataset missing telemetry archive at telemetry/ecosystem-events.log`).
- `ritualist-heavy` existing committed analytics artifacts: analysis report=yes, job record=yes, output manifest=yes, run metadata=yes, recommendation history=yes.
  - Fresh pipeline rerun status: FAILED (`Harness dataset missing telemetry archive at telemetry/ecosystem-events.log`).
- `gatherer-heavy` existing committed analytics artifacts: analysis report=yes, job record=yes, output manifest=yes, run metadata=yes, recommendation history=yes.
  - Fresh pipeline rerun status: FAILED (`Harness dataset missing telemetry archive at telemetry/ecosystem-events.log`).
- `mixed` existing committed analytics artifacts: analysis report=yes, job record=yes, output manifest=yes, run metadata=yes, recommendation history=yes.
  - Fresh pipeline rerun status: FAILED (`Harness dataset missing telemetry archive at telemetry/ecosystem-events.log`).
- `random-baseline` existing committed analytics artifacts: analysis report=yes, job record=yes, output manifest=yes, run metadata=yes, recommendation history=yes.
  - Fresh pipeline rerun status: FAILED (`Harness dataset missing telemetry archive at telemetry/ecosystem-events.log`).

SECTION 4: TELEMETRY / ROLLUP INGESTION COHERENCE
- `explorer-heavy` prior successful ingestion: telemetry_events=63110, rollups_loaded=1, window_size=1.
- `ritualist-heavy` prior successful ingestion: telemetry_events=62200, rollups_loaded=1, window_size=1.
- `gatherer-heavy` prior successful ingestion: telemetry_events=63596, rollups_loaded=1, window_size=1.
- `mixed` prior successful ingestion: telemetry_events=69346, rollups_loaded=1, window_size=1.
- `random-baseline` prior successful ingestion: telemetry_events=62950, rollups_loaded=1, window_size=1.
- Coherence gap: all five scenario run folders currently lack required `telemetry/ecosystem-events.log`, so current datasets are contract-incompatible for fresh ingestion.

SECTION 5: ANOMALY / RECOMMENDATION OUTPUTS
- `explorer-heavy`: severity=0.000, runaway_lineages=[], niche_collapse=[], recommendation=validation-explorer-heavy-2224e19a-6636-429b-a943-b23e8ffdf5b0 (PROPOSED).
- `ritualist-heavy`: severity=0.000, runaway_lineages=[], niche_collapse=[], recommendation=validation-ritualist-heavy-8aafd1c6-37f9-4807-a94d-9ba1f8f76d79 (PROPOSED).
- `gatherer-heavy`: severity=0.000, runaway_lineages=[], niche_collapse=[], recommendation=validation-gatherer-heavy-0c4dd65b-e201-43d3-b5dc-b68a8d9366c0 (PROPOSED).
- `mixed`: severity=0.000, runaway_lineages=[], niche_collapse=[], recommendation=validation-mixed-db8fc441-4d61-4e16-9257-de7503ed4a4e (PROPOSED).
- `random-baseline`: severity=0.000, runaway_lineages=[], niche_collapse=[], recommendation=validation-random-baseline-4c641e9f-7b52-4300-b331-47c001aa74ea (PROPOSED).

SECTION 6: HISTORICAL-DEPTH / WINDOW ANALYSIS STATUS
- `explorer-heavy`: Insufficient historical rollups for long-term analysis.
- `ritualist-heavy`: Insufficient historical rollups for long-term analysis.
- `gatherer-heavy`: Insufficient historical rollups for long-term analysis.
- `mixed`: Insufficient historical rollups for long-term analysis.
- `random-baseline`: Insufficient historical rollups for long-term analysis.
- Historical depth result: insufficient in every scenario (single rollup/window), preventing long-horizon trend inference.

SECTION 7: ANALYTICS FAILURES OR CONTRACT GAPS
- Hard failure: `TelemetryDatasetContract`/`HarnessOutputAdapter` require `telemetry/ecosystem-events.log`; artifact missing in all scenario datasets.
- Data contract gap: `rollup-snapshots.json` contains non-JSON object literals, so strict JSON ingestion is not possible without transformation.
- Resulting impact: analytics rerun cannot produce new analysis reports/job records/manifests/run metadata/recommendation histories from current harness outputs.

SECTION 8: ANALYTICS READINESS VERDICT
PARTIALLY COHERENT
- Reason: previously generated analytics outputs are internally coherent, but fresh harness outputs in-repo are not contract-complete (missing telemetry archive), so ingestion pipeline is currently blocked.
