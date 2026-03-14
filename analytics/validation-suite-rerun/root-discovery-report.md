SECTION 1: CANDIDATE DATASET ROOTS FOUND

Preferred harness markers searched:
- scenario-metadata.properties
- rollup-snapshots.json

Result:
- No files matching either preferred marker were found anywhere in the repository snapshot.

Fallback candidate roots discovered from scenario-layout and harness-analysis artifacts:

1) analytics/validation-suite/analysis
- Contains all expected scenario directories:
  - explorer-heavy
  - ritualist-heavy
  - gatherer-heavy
  - mixed
  - random-baseline
- Contains per-scenario harness-analysis artifacts, including:
  - validation-*-run-metadata.properties
  - validation-*-output-manifest.properties
  - validation-*-analysis-report.txt

2) analytics/validation-suite-fresh/live-analysis-attempt
- Contains all expected scenario directories.
- Contains per-scenario cli.log files and a top-level cli-results.tsv.
- Logs show failures caused by missing telemetry archive (not complete harness outputs).

3) analytics/validation-suite-rerun/archive-fix-rerun-20260314-003623
- Contains execution-report.md only.
- Does not contain scenario subdirectories in this snapshot.

4) analytics/validation-suite-rerun (report-indicated candidate)
- Contains execution-report-20260314-005706.md that references archive-fix-rerun-20260314-005706/runs/*.
- The referenced archive-fix-rerun-20260314-005706 directory is not present in this repository snapshot.

SECTION 2: FRESH DATASET ROOT SELECTED

Selected root:
analytics/validation-suite/analysis

Freshness determination basis:
- It is the most recent physically present and scenario-complete root with usable run metadata/manifests.
- Per-scenario run metadata shows successful completion and timestamps (startedAtMs/finishedAtMs).
- Newer rerun reporting files exist, but their referenced dataset root is missing from disk, so it cannot be selected as an on-filesystem dataset root.

SECTION 3: SCENARIO ROOT MAPPING

explorer-heavy -> analytics/validation-suite/analysis/explorer-heavy
ritualist-heavy -> analytics/validation-suite/analysis/ritualist-heavy
gatherer-heavy -> analytics/validation-suite/analysis/gatherer-heavy
mixed -> analytics/validation-suite/analysis/mixed
random-baseline -> analytics/validation-suite/analysis/random-baseline

SECTION 4: ROOT DISCOVERY VERDICT

PARTIAL

Rationale:
- Scenario-complete dataset root is available and mapped.
- Preferred marker files (scenario-metadata.properties, rollup-snapshots.json) are absent.
- Report-indicated newer rerun dataset root is referenced but not present in the repository snapshot.
