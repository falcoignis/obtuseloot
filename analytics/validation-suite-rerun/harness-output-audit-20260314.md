SECTION 1: DISCOVERED SCENARIO ROOTS

Most recent run evidence:
- The freshest run manifest-like artifact is `analytics/validation-suite-rerun/execution-report-20260314-005706.md` (mtime 2026-03-14 01:04:17), newer than all scenario artifacts under `analytics/validation-suite*` (mtime ~2026-03-13 23:08:20).
- That report declares outputs root: `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706/runs`.
- That declared root does not currently exist on disk.

Secondary/stale candidates considered:
- `analytics/validation-suite-rerun/archive-fix-rerun-20260314-003623` exists, but only contains an execution report and no `runs/<scenario>` tree.
- `analytics/validation-suite-fresh/live-analysis-attempt/<scenario>` exists but is an analytics CLI attempt directory (contains only `cli.log`), not harness scenario output roots.
- `analytics/validation-suite/analysis/<scenario>` exists but contains post-analysis artifacts only, not harness dataset roots.

Per-scenario root discovery (most recent run target):
- explorer-heavy: expected `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706/runs/explorer-heavy` (NOT FOUND)
- ritualist-heavy: expected `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706/runs/ritualist-heavy` (NOT FOUND)
- gatherer-heavy: expected `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706/runs/gatherer-heavy` (NOT FOUND)
- mixed: expected `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706/runs/mixed` (NOT FOUND)
- random-baseline: expected `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706/runs/random-baseline` (NOT FOUND)

SECTION 2: ARTIFACT COMPLETENESS

Required artifacts checked per scenario under the discovered/expected fresh roots:
- telemetry/ecosystem-events.log
- telemetry/rollup-snapshot.properties
- rollup-snapshots.json
- scenario-metadata.properties

Result:
- All scenarios: NOT PRESENT because the discovered/expected fresh scenario root directories are absent.

Cross-check against present candidate directories:
- `analytics/validation-suite-fresh/live-analysis-attempt/<scenario>`: missing all required harness artifacts (contains `cli.log` only).
- `analytics/validation-suite/analysis/<scenario>`: contains analysis outputs, but does not contain required harness dataset contract files.

SECTION 3: TELEMETRY ARCHIVE NON-EMPTY CHECK

Fresh scenario roots:
- Could not validate non-empty telemetry archives because `telemetry/ecosystem-events.log` files are absent with missing scenario roots.

Present candidate directories:
- No `ecosystem-events.log` file found under repository workspace during global search.
- `live-analysis-attempt/*/cli.log` explicitly reports: "Harness dataset missing telemetry archive at telemetry/ecosystem-events.log".

SECTION 4: LIFECYCLE TELEMETRY PRESENCE

Target checks:
- branch lifecycle state
- branch survival score
- maintenance cost
- collapse transitions
- branch collapse events

Result:
- Not detectable for all scenarios because no telemetry archive file was found to parse.
- Absence reason: missing/absent telemetry archive, not a parsing-format mismatch.

SECTION 5: DATASET COHERENCE

Analytics-ready criteria:
- all required artifacts exist
- telemetry archive non-empty
- paths align with dataset contract

Scenario readiness:
- explorer-heavy: NOT analytics-ready (required dataset root missing)
- ritualist-heavy: NOT analytics-ready (required dataset root missing)
- gatherer-heavy: NOT analytics-ready (required dataset root missing)
- mixed: NOT analytics-ready (required dataset root missing)
- random-baseline: NOT analytics-ready (required dataset root missing)

SECTION 6: ARTIFACT VERDICT

BROKEN

Rationale:
- Most recent run points to a fresh output root that is not present on disk.
- Required artifacts are broadly absent.
- Telemetry archives required for lifecycle validation are absent/unusable.
