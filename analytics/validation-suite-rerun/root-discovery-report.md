SECTION 1: LATEST RUN POINTER STATUS

- `analytics/validation-suite/latest-run.properties`: NOT FOUND.
- Phase 1 pointer validation result: invalid (pointer file missing), so Phase 2 discovery heuristics were used.

SECTION 2: CANDIDATE DATASET ROOTS FOUND

Discovery rules used:
- Preferred markers: `scenario-metadata.properties`, `rollup-snapshots.json`.
- Supplemental harness indicators: per-scenario run metadata/manifests, scenario directory matrix, and execution reports.
- Rejected roots: locations containing only analysis artifacts or CLI logs.

Candidates identified:

1) `analytics/validation-suite/analysis`
- Contains all expected scenario directories:
  - `explorer-heavy`
  - `ritualist-heavy`
  - `gatherer-heavy`
  - `mixed`
  - `random-baseline`
- Contains per-scenario harness analysis outputs:
  - `validation-*-run-metadata.properties`
  - `validation-*-output-manifest.properties`
  - `validation-*-analysis-report.txt`
- Preferred markers are absent, but this is the only physically present scenario-complete root with run metadata.

2) `analytics/validation-suite-fresh/live-analysis-attempt`
- Contains all expected scenario directories, each with `cli.log`, plus top-level `cli-results.tsv`.
- Rejected as dataset root candidate for validation because it contains CLI attempt logs rather than harness scenario output artifacts.

3) `analytics/validation-suite-rerun/archive-fix-rerun-20260314-003623`
- Contains only `execution-report.md` and no scenario output directory tree.
- Rejected as dataset root candidate.

4) Report-referenced but missing on disk:
- `analytics/validation-suite-rerun/archive-fix-rerun-20260314-005706`
- Referenced by `execution-report-20260314-005706.md` as a full successful harness run root, but directory is not present in this repository snapshot.

SECTION 3: FRESH DATASET ROOT SELECTED

Selected root: `analytics/validation-suite/analysis`

Why this root was selected:
- It is present on disk and scenario-complete for the expected five-scenario validation matrix.
- Per-scenario run metadata indicates successful executions (`status=SUCCESS`) with ordered run timestamps (`startedAtMs`/`finishedAtMs`) across all scenarios.
- A newer rerun is documented (`execution-report-20260314-005706.md`) and preferred by reported execution time, but its referenced dataset root is missing from disk, so it cannot be selected as the repository-local validation dataset root.

SECTION 4: SCENARIO ROOT MAPPING

explorer-heavy -> `analytics/validation-suite/analysis/explorer-heavy`
ritualist-heavy -> `analytics/validation-suite/analysis/ritualist-heavy`
gatherer-heavy -> `analytics/validation-suite/analysis/gatherer-heavy`
mixed -> `analytics/validation-suite/analysis/mixed`
random-baseline -> `analytics/validation-suite/analysis/random-baseline`

SECTION 5: ROOT DISCOVERY VERDICT

PARTIAL

Reason:
- A repository-local, scenario-complete root was found and mapped.
- Phase 1 pointer file is missing.
- Preferred marker files (`scenario-metadata.properties`, `rollup-snapshots.json`, `telemetry/ecosystem-events.log`) are absent from all physically present roots.
- The report-indicated latest rerun root exists only as a reference in execution reporting and is not present on disk.
