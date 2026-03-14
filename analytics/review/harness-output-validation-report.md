# Harness Output Validation Report

## Section 1 — Discover Actual Scenario Roots

Candidate scenario root directories (newest first):
- `analytics/validation-suite/runs` (latest scenario dir mtime: 2026-03-13 22:42:20 UTC)
- `analytics/validation-suite/analysis` (latest scenario dir mtime: 2026-03-13 22:42:20 UTC)
- `analytics/validation-suite-fresh/live-analysis-attempt` (latest scenario dir mtime: 2026-03-13 22:42:20 UTC)

Most recent root selected: `analytics/validation-suite/runs`

- `explorer-heavy` → `analytics/validation-suite/runs/explorer-heavy` (dir mtime: 2026-03-13 22:42:20 UTC)
- `ritualist-heavy` → `analytics/validation-suite/runs/ritualist-heavy` (dir mtime: 2026-03-13 22:42:20 UTC)
- `gatherer-heavy` → `analytics/validation-suite/runs/gatherer-heavy` (dir mtime: 2026-03-13 22:42:20 UTC)
- `mixed` → `analytics/validation-suite/runs/mixed` (dir mtime: 2026-03-13 22:42:20 UTC)
- `random-baseline` → `analytics/validation-suite/runs/random-baseline` (dir mtime: 2026-03-13 22:42:20 UTC)

Freshness/correctness evidence:
- `analytics/validation-suite-fresh/live-analysis-attempt/*/cli.log` contains immediate failures complaining that telemetry archive is missing from harness datasets, indicating these were analysis attempts against harness outputs rather than scenario outputs themselves.
- `analytics/validation-suite/runs/*` contains full scenario datasets (`scenario-metadata.properties`, `rollup-snapshots.json`, telemetry folder), unlike `validation-suite-fresh` which only has `cli.log`.

## Section 2 — Required Artifact Check

| Scenario | Root | ecosystem-events.log | non-empty | rollup-snapshot.properties | rollup-snapshots.json | scenario-metadata.properties | Complete? |
|---|---|---|---|---|---|---|---|
| explorer-heavy | `analytics/validation-suite/runs/explorer-heavy` | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ |
| ritualist-heavy | `analytics/validation-suite/runs/ritualist-heavy` | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ |
| gatherer-heavy | `analytics/validation-suite/runs/gatherer-heavy` | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ |
| mixed | `analytics/validation-suite/runs/mixed` | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ |
| random-baseline | `analytics/validation-suite/runs/random-baseline` | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ |

## Section 3 — Lifecycle Telemetry Check

Telemetry archive required path: `telemetry/ecosystem-events.log` under each scenario root.

- `explorer-heavy`: ❌ Cannot validate lifecycle signals because `analytics/validation-suite/runs/explorer-heavy/telemetry/ecosystem-events.log` is missing.
- `ritualist-heavy`: ❌ Cannot validate lifecycle signals because `analytics/validation-suite/runs/ritualist-heavy/telemetry/ecosystem-events.log` is missing.
- `gatherer-heavy`: ❌ Cannot validate lifecycle signals because `analytics/validation-suite/runs/gatherer-heavy/telemetry/ecosystem-events.log` is missing.
- `mixed`: ❌ Cannot validate lifecycle signals because `analytics/validation-suite/runs/mixed/telemetry/ecosystem-events.log` is missing.
- `random-baseline`: ❌ Cannot validate lifecycle signals because `analytics/validation-suite/runs/random-baseline/telemetry/ecosystem-events.log` is missing.

## Section 4 — Reporting Summary

- Selected most recent scenario output root: `analytics/validation-suite/runs`.
- Artifact completeness: all scenarios are incomplete due to missing `telemetry/ecosystem-events.log`.
- Telemetry archive validation: failed for all scenarios (archive missing).
- Lifecycle telemetry validation: not possible without the archive file.
- Analytics-readiness: ❌ not analytics-ready for all scenarios because telemetry archive precondition fails.
