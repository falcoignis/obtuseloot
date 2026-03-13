# Harness Artifact Audit

## SECTION 1: ARTIFACT LOCATIONS

- `explorer-heavy`: `analytics/validation-suite/runs/explorer-heavy`
- `gatherer-heavy`: `analytics/validation-suite/runs/gatherer-heavy`
- `mixed`: `analytics/validation-suite/runs/mixed`
- `random-baseline`: `analytics/validation-suite/runs/random-baseline`
- `ritualist-heavy`: `analytics/validation-suite/runs/ritualist-heavy`

## SECTION 2: ARTIFACT COMPLETENESS

| Scenario | telemetry/ecosystem-events.log | telemetry/rollup-snapshot.properties | rollup-snapshots.json | scenario-metadata.properties | Status |
|---|---|---|---|---|---|
| explorer-heavy | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| gatherer-heavy | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| mixed | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| random-baseline | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| ritualist-heavy | ❌ | ✅ | ✅ | ✅ | PARTIAL |

## SECTION 3: LIFECYCLE TELEMETRY PRESENCE

| Scenario | branch lifecycle state | branch survival score | maintenance cost | collapse transitions | branch collapse events | Status |
|---|---|---|---|---|---|---|
| explorer-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | BROKEN |
| gatherer-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | BROKEN |
| mixed | ❌ | ❌ | ❌ | ❌ | ❌ | BROKEN |
| random-baseline | ❌ | ❌ | ❌ | ❌ | ❌ | BROKEN |
| ritualist-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | BROKEN |

## SECTION 4: ARTIFACT VERDICT

**BROKEN**

_Validation method_: exact filename and exact phrase matching (case-insensitive for phrases).
