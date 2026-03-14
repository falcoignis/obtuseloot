# Root Discovery Report

## SECTION 1: POINTER FILE STATUS
- `analytics/validation-suite/latest-run.properties` exists.
- `dataset_root` value: `/workspace/ObtuseLoot/analytics/validation-suite-rerun/runs`.
- Pointer root directory check: **missing**.
- Because pointer root is missing, strict scenario artifact contract fails.

## SECTION 2: CANDIDATE ROOTS DISCOVERED
Candidates with at least three expected scenario directories discovered in repository scan:
- `/workspace/ObtuseLoot/analytics/validation-suite-fresh/live-analysis-attempt` (5/5 scenario names found)
- `/workspace/ObtuseLoot/analytics/validation-suite/analysis` (5/5 scenario names found)

## SECTION 3: VALID ROOT CHECK
Strict harness artifact contract required in each scenario directory:
- `telemetry/ecosystem-events.log`
- `telemetry/rollup-snapshot.properties`
- `rollup-snapshots.json`
- `scenario-metadata.properties`

Result:
- Pointer root: invalid (directory missing)
- Candidate root `/workspace/ObtuseLoot/analytics/validation-suite-fresh/live-analysis-attempt`: invalid (required artifacts missing in every scenario)
- Candidate root `/workspace/ObtuseLoot/analytics/validation-suite/analysis`: invalid (required artifacts missing in every scenario)

## SECTION 4: SCENARIO ROOT MAPPING
Expected scenario names:
- `explorer-heavy`
- `ritualist-heavy`
- `gatherer-heavy`
- `mixed`
- `random-baseline`

Mapping outcomes:
- Pointer root mapping: unavailable because root directory is absent.
- Candidate roots: all five scenario directories exist, but each fails artifact presence checks.

## SECTION 5: ROOT DISCOVERY VERDICT
**BROKEN**

No valid dataset root satisfies the strict scenario artifact contract. Per instructions, `latest-run.properties` was not modified.
