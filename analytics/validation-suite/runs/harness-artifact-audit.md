# Harness Artifact Audit

## Phase 1 — Artifact Discovery

Scenario output directories discovered under `analytics/validation-suite/runs`:

- `explorer-heavy`: `analytics/validation-suite/runs/explorer-heavy`
- `gatherer-heavy`: `analytics/validation-suite/runs/gatherer-heavy`
- `mixed`: `analytics/validation-suite/runs/mixed`
- `random-baseline`: `analytics/validation-suite/runs/random-baseline`
- `ritualist-heavy`: `analytics/validation-suite/runs/ritualist-heavy`

## Phase 2 — Artifact Validation

| Scenario | telemetry/ecosystem-events.log | telemetry/rollup-snapshot.properties | rollup-snapshots.json | scenario-metadata.properties | Status |
|---|---|---|---|---|---|
| explorer-heavy | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| gatherer-heavy | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| mixed | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| random-baseline | ❌ | ✅ | ✅ | ✅ | PARTIAL |
| ritualist-heavy | ❌ | ✅ | ✅ | ✅ | PARTIAL |

## Phase 3 — Lifecycle Telemetry Validation

| Scenario | branch lifecycle state | branch survival score | maintenance cost | collapse transitions | branch collapse events | Status |
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

All scenarios are **PARTIAL** because `telemetry/ecosystem-events.log` is missing in every scenario directory.

## SECTION 3: LIFECYCLE TELEMETRY PRESENCE

All scenarios are **BROKEN** for lifecycle telemetry because none of the required signals were found via exact phrase matching.

## SECTION 4: ARTIFACT VERDICT

**BROKEN**

_Validation method_: exact filename checks and case-insensitive exact phrase matching across scenario-local `.log`, `.properties`, `.json`, `.md`, and `.txt` files.
