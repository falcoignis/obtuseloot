# Harness Artifact Audit

## Phase 1 — Artifact Discovery

Located scenario output directories under `analytics/validation-suite/runs`:

- `analytics/validation-suite/runs/explorer-heavy`
- `analytics/validation-suite/runs/gatherer-heavy`
- `analytics/validation-suite/runs/mixed`
- `analytics/validation-suite/runs/random-baseline`
- `analytics/validation-suite/runs/ritualist-heavy`

## Phase 2 — Artifact Validation

| Scenario | telemetry/ecosystem-events.log | telemetry/rollup-snapshot.properties | rollup-snapshots.json | scenario-metadata.properties | Completeness |
|---|---|---|---|---|---|
| explorer-heavy | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| gatherer-heavy | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| mixed | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| random-baseline | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| ritualist-heavy | ❌ | ✅ | ✅ | ✅ | PARTIAL |

## Phase 3 — Lifecycle Telemetry Validation

Checked `telemetry/ecosystem-events.log` for these required signals:

- `branch lifecycle state`
- `branch survival score`
- `maintenance cost`
- `collapse transitions`
- `branch collapse events`

| Scenario | branch lifecycle state | branch survival score | maintenance cost | collapse transitions | branch collapse events | Telemetry Presence |
|---|---|---|---|---|---|---|
| explorer-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | BROKEN |
| gatherer-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | BROKEN |
| mixed | ❌ | ❌ | ❌ | ❌ | ❌ | BROKEN |
| random-baseline | ❌ | ❌ | ❌ | ❌ | ❌ | BROKEN |
| ritualist-heavy | ❌ | ❌ | ❌ | ❌ | ❌ | BROKEN |

## SECTION 1: ARTIFACT LOCATIONS

- `analytics/validation-suite/runs/explorer-heavy`
- `analytics/validation-suite/runs/gatherer-heavy`
- `analytics/validation-suite/runs/mixed`
- `analytics/validation-suite/runs/random-baseline`
- `analytics/validation-suite/runs/ritualist-heavy`

## SECTION 2: ARTIFACT COMPLETENESS

All discovered scenarios are **PARTIAL** because `telemetry/ecosystem-events.log` is missing in each scenario output directory.

## SECTION 3: LIFECYCLE TELEMETRY PRESENCE

Lifecycle telemetry is **BROKEN** for all discovered scenarios because none of the required lifecycle signals are present in `telemetry/ecosystem-events.log` (file missing in each scenario).

## SECTION 4: ARTIFACT VERDICT

**BROKEN**
