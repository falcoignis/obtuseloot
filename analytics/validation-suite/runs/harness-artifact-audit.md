# Harness Artifact Audit

## SECTION 1: DISCOVERED SCENARIO ROOTS

### Candidate roots evaluated

| Candidate base | Evidence | Freshness/correctness determination |
|---|---|---|
| `analytics/validation-suite/runs` | Contains per-scenario `scenario-metadata.properties`, `rollup-snapshots.json`, and `telemetry/rollup-snapshot.properties`; scenario dir mtimes are `2026-03-13 23:02:26.898...` to `23:02:26.902...` UTC. | **Selected** as the most recent valid harness dataset root family. |
| `analytics/validation-suite-fresh/live-analysis-attempt` | Contains only `cli.log` files plus `cli-results.tsv`; each `cli.log` records `Harness dataset missing telemetry archive at telemetry/ecosystem-events.log`. Scenario dir mtimes are `2026-03-13 23:02:26.894...` UTC (slightly older). | Rejected as harness output roots (analysis-attempt logs only, not datasets). |
| `analytics/validation-suite/analysis` | Contains downstream analysis outputs, not dataset-contract files for harness ingestion. | Rejected (post-processing artifacts, not harness roots). |

### Discovered scenario roots for the most recent harness dataset set

- `explorer-heavy` → `analytics/validation-suite/runs/explorer-heavy`
- `ritualist-heavy` → `analytics/validation-suite/runs/ritualist-heavy`
- `gatherer-heavy` → `analytics/validation-suite/runs/gatherer-heavy`
- `mixed` → `analytics/validation-suite/runs/mixed`
- `random-baseline` → `analytics/validation-suite/runs/random-baseline`

## SECTION 2: ARTIFACT COMPLETENESS

Required artifact checks:
- `telemetry/ecosystem-events.log`
- `telemetry/rollup-snapshot.properties`
- `rollup-snapshots.json`
- `scenario-metadata.properties`

| Scenario | ecosystem-events.log | rollup-snapshot.properties | rollup-snapshots.json | scenario-metadata.properties | Required set complete? |
|---|---|---|---|---|---|
| explorer-heavy | ❌ missing | ✅ present | ✅ present | ✅ present | ❌ |
| ritualist-heavy | ❌ missing | ✅ present | ✅ present | ✅ present | ❌ |
| gatherer-heavy | ❌ missing | ✅ present | ✅ present | ✅ present | ❌ |
| mixed | ❌ missing | ✅ present | ✅ present | ✅ present | ❌ |
| random-baseline | ❌ missing | ✅ present | ✅ present | ✅ present | ❌ |

## SECTION 3: TELEMETRY ARCHIVE NON-EMPTY CHECK

Target archive: `telemetry/ecosystem-events.log`

| Scenario | Exists | Non-empty | Evidence |
|---|---|---|---|
| explorer-heavy | ❌ | ❌ | Archive path absent |
| ritualist-heavy | ❌ | ❌ | Archive path absent |
| gatherer-heavy | ❌ | ❌ | Archive path absent |
| mixed | ❌ | ❌ | Archive path absent |
| random-baseline | ❌ | ❌ | Archive path absent |

## SECTION 4: LIFECYCLE TELEMETRY PRESENCE

Requested lifecycle signals:
- branch lifecycle state
- branch survival score
- maintenance cost
- collapse transitions
- branch collapse events

Structured parsing preference outcome:
- No parsing was possible because `telemetry/ecosystem-events.log` is missing for every discovered scenario root.
- Signal absence is due to **missing archive files**, not due to parser mismatch.

| Scenario | Lifecycle state | Survival score | Maintenance cost | Collapse transitions | Branch collapse events | Reason |
|---|---|---|---|---|---|---|
| explorer-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | No telemetry archive |
| ritualist-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | No telemetry archive |
| gatherer-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | No telemetry archive |
| mixed | ❌ | ❌ | ❌ | ❌ | ❌ | No telemetry archive |
| random-baseline | ❌ | ❌ | ❌ | ❌ | ❌ | No telemetry archive |

## SECTION 5: DATASET COHERENCE

Analytics-ready requires all three:
1. all required artifacts exist,
2. telemetry archive exists and is non-empty,
3. dataset contract paths align.

| Scenario | Required artifacts complete | Archive non-empty | Contract aligned | Analytics-ready |
|---|---|---|---|---|
| explorer-heavy | ❌ | ❌ | ❌ | ❌ |
| ritualist-heavy | ❌ | ❌ | ❌ | ❌ |
| gatherer-heavy | ❌ | ❌ | ❌ | ❌ |
| mixed | ❌ | ❌ | ❌ | ❌ |
| random-baseline | ❌ | ❌ | ❌ | ❌ |

## SECTION 6: ARTIFACT VERDICT

**PARTIAL**

Reason:
- Correct/fresh harness scenario roots were identified by timestamp and artifact-shape evidence.
- Some required artifacts are present.
- Telemetry archive `telemetry/ecosystem-events.log` is missing for all scenarios, so lifecycle telemetry cannot be validated and no scenario is analytics-ready.
