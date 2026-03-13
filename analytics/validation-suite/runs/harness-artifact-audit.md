# Harness Artifact Audit (Most Recent Run Selection + Dataset Contract Check)

Audit scope requested:
- `explorer-heavy`
- `ritualist-heavy`
- `gatherer-heavy`
- `mixed`
- `random-baseline`

Method highlights:
- Enumerated candidate scenario roots under `analytics/*`.
- Compared freshness by latest file modification time per base root.
- Cross-checked intent using CLI failure logs in `analytics/validation-suite-fresh/live-analysis-attempt/*/cli.log`.
- Validated required artifact contract against the freshest harness scenario roots.

## SECTION 1: DISCOVERED SCENARIO ROOTS

### Candidate bases discovered

| Candidate base | Latest observed mtime | Notes |
|---|---:|---|
| `analytics/validation-suite/runs` | `2026-03-13 23:48:31` | Contains harness datasets (`scenario-metadata.properties`, `rollup-snapshots.json`, telemetry subtree). |
| `analytics/validation-suite-fresh/live-analysis-attempt` | `2026-03-13 22:37:56` | Contains only analytics CLI outputs (`cli.log`), not full harness datasets. |
| `analytics/validation-suite/analysis` | `2026-03-13 22:37:56` | Contains downstream analysis artifacts, not harness raw dataset roots. |

### Most-recent harness scenario roots selected

Freshest valid harness base: **`analytics/validation-suite/runs`** (newer than other candidates and contains dataset-contract files).

| Scenario | Discovered root |
|---|---|
| explorer-heavy | `analytics/validation-suite/runs/explorer-heavy` |
| ritualist-heavy | `analytics/validation-suite/runs/ritualist-heavy` |
| gatherer-heavy | `analytics/validation-suite/runs/gatherer-heavy` |
| mixed | `analytics/validation-suite/runs/mixed` |
| random-baseline | `analytics/validation-suite/runs/random-baseline` |

Evidence for rejecting stale/incorrect roots:
- `live-analysis-attempt/*/cli.log` records analytics ingestion failures caused by missing `telemetry/ecosystem-events.log`, indicating this path is analysis output, not source harness root.
- `analysis/*` contains `validation-*-analysis-report.txt` and job metadata, indicating post-processing output.

## SECTION 2: ARTIFACT COMPLETENESS

Required artifacts checked per scenario:
- `telemetry/ecosystem-events.log`
- `telemetry/rollup-snapshot.properties`
- `rollup-snapshots.json`
- `scenario-metadata.properties`

| Scenario | ecosystem-events.log | rollup-snapshot.properties | rollup-snapshots.json | scenario-metadata.properties | Completeness |
|---|---|---|---|---|---|
| explorer-heavy | ❌ missing | ✅ present | ✅ present | ✅ present | PARTIAL |
| ritualist-heavy | ❌ missing | ✅ present | ✅ present | ✅ present | PARTIAL |
| gatherer-heavy | ❌ missing | ✅ present | ✅ present | ✅ present | PARTIAL |
| mixed | ❌ missing | ✅ present | ✅ present | ✅ present | PARTIAL |
| random-baseline | ❌ missing | ✅ present | ✅ present | ✅ present | PARTIAL |

## SECTION 3: TELEMETRY ARCHIVE NON-EMPTY CHECK

Target archive: `telemetry/ecosystem-events.log`

| Scenario | Archive exists? | Non-empty? | Evidence |
|---|---|---|---|
| explorer-heavy | ❌ | ❌ | File absent |
| ritualist-heavy | ❌ | ❌ | File absent |
| gatherer-heavy | ❌ | ❌ | File absent |
| mixed | ❌ | ❌ | File absent |
| random-baseline | ❌ | ❌ | File absent |

Result: no scenario passed archive non-empty validation because the archive file is missing in all selected fresh harness roots.

## SECTION 4: LIFECYCLE TELEMETRY PRESENCE

Lifecycle signals requested:
- branch lifecycle state
- branch survival score
- maintenance cost
- collapse transitions
- branch collapse events

Validation approach:
- Preferred structured parsing of `telemetry/ecosystem-events.log` if present.
- Fallback key search only if structured parsing is not possible.

Observed outcome:
- Structured parsing could not be executed for any scenario because `telemetry/ecosystem-events.log` is absent.
- Therefore none of the lifecycle signals are detectable in the audited archives.

| Scenario | lifecycle state | survival score | maintenance cost | collapse transitions | collapse events | Reason |
|---|---|---|---|---|---|---|
| explorer-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | Archive missing |
| ritualist-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | Archive missing |
| gatherer-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | Archive missing |
| mixed | ❌ | ❌ | ❌ | ❌ | ❌ | Archive missing |
| random-baseline | ❌ | ❌ | ❌ | ❌ | ❌ | Archive missing |

## SECTION 5: DATASET COHERENCE

Analytics-ready rule:
- all required artifacts exist,
- telemetry archive exists and is non-empty,
- dataset paths satisfy contract.

| Scenario | All required artifacts? | Archive non-empty? | Contract-aligned for ingestion? | Analytics-ready |
|---|---|---|---|---|
| explorer-heavy | ❌ | ❌ | ❌ | ❌ |
| ritualist-heavy | ❌ | ❌ | ❌ | ❌ |
| gatherer-heavy | ❌ | ❌ | ❌ | ❌ |
| mixed | ❌ | ❌ | ❌ | ❌ |
| random-baseline | ❌ | ❌ | ❌ | ❌ |

## SECTION 6: ARTIFACT VERDICT

**PARTIAL**

Rationale:
- Correct fresh harness roots were identified.
- Several required artifacts are present for every scenario.
- Critical telemetry archive (`telemetry/ecosystem-events.log`) is missing for all scenarios, so lifecycle telemetry cannot be validated and datasets are not analytics-ready.
