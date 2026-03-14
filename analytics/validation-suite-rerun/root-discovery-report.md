SECTION 1: CANDIDATE DATASET ROOTS FOUND

Discovery criteria applied:
- Preferred harness markers: `scenario-metadata.properties`, `rollup-snapshots.json`.
- Freshness evidence: execution reports, run metadata/manifests, and file timestamps.

Candidates:

1) `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706` (report-referenced)
- Referenced by `execution-report-20260314-005706.md` as the run root for a successful full matrix rerun.
- Report states all expected scenario outputs were written under `runs/<scenario>` and include preferred markers.
- Directory is not present on disk in this repository snapshot.

2) `analytics/validation-suite/analysis` (present on disk)
- Scenario-complete root with directories for:
  - `explorer-heavy`
  - `ritualist-heavy`
  - `gatherer-heavy`
  - `mixed`
  - `random-baseline`
- Contains per-scenario run metadata and output manifests:
  - `validation-*-run-metadata.properties`
  - `validation-*-output-manifest.properties`
- Does not contain preferred marker filenames at this root.

3) `analytics/validation-suite-fresh/live-analysis-attempt` (present on disk)
- Scenario-complete directory matrix with per-scenario `cli.log` files and top-level `cli-results.tsv`.
- Classified as CLI-attempt logs, not as harness dataset output root.

SECTION 2: FRESH DATASET ROOT SELECTED

Most recent harness run root (by execution report evidence):
- `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706`
- Basis: `execution-report-20260314-005706.md` declares successful completion for all five scenarios and scenario-local output population.
- Constraint: this root is missing on disk.

Repository-local fallback root for validation inputs:
- `analytics/validation-suite/analysis`
- Basis: physically present, scenario-complete, and per-scenario metadata indicates `status=SUCCESS` with valid start/finish timestamps.

SECTION 3: SCENARIO ROOT MAPPING

Using repository-local fallback root `analytics/validation-suite/analysis`:

- explorer-heavy -> `analytics/validation-suite/analysis/explorer-heavy`
- ritualist-heavy -> `analytics/validation-suite/analysis/ritualist-heavy`
- gatherer-heavy -> `analytics/validation-suite/analysis/gatherer-heavy`
- mixed -> `analytics/validation-suite/analysis/mixed`
- random-baseline -> `analytics/validation-suite/analysis/random-baseline`

SECTION 4: ROOT DISCOVERY VERDICT

PARTIAL

Reasoning:
- Candidate roots were discovered and mapped to expected scenarios.
- The freshest harness run root is identifiable from execution reporting but absent from the checked-in filesystem.
- A scenario-complete fallback root exists locally and can be used for validation, though it lacks preferred marker filenames (`scenario-metadata.properties`, `rollup-snapshots.json`).
