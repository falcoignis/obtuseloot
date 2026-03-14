SECTION 1: CANDIDATE DATASET ROOTS FOUND

1) analytics/validation-suite/analysis
- Contains all five expected scenario directories.
- Contains per-scenario run metadata and output manifest artifacts (`validation-*-run-metadata.properties`, `validation-*-output-manifest.properties`).

2) analytics/validation-suite-fresh/live-analysis-attempt
- Contains all five expected scenario directories.
- Contains per-scenario `cli.log` files, but no run metadata/manifest artifacts.

3) analytics/validation-suite-rerun/archive-fix-rerun-20260314-003623
- Contains rerun execution metadata (`execution-report.md`) and references a `runs/*` layout.
- In this repository snapshot, no scenario subdirectories are present under this archive root.

4) analytics/validation-suite-rerun (report-indicated candidate)
- Contains rerun reports (`execution-report-20260314-005706.md`, `harness-output-audit-20260314.md`) that reference `archive-fix-rerun-20260314-005706/runs/*`.
- The referenced `archive-fix-rerun-20260314-005706` directory is not present in this repository snapshot.

SECTION 2: FRESH DATASET ROOT SELECTED

Selected root for immediate validation mapping: `analytics/validation-suite/analysis`.

Why:
- It is the most recent dataset root that is physically present and fully scenario-complete for the expected matrix.
- Each expected scenario directory exists and includes analysis manifests/metadata usable for downstream validation.
- Newer rerun reports exist, but their referenced run roots are missing from the current filesystem snapshot, so they cannot be used as concrete dataset roots.

SECTION 3: SCENARIO ROOT MAPPING

explorer-heavy -> analytics/validation-suite/analysis/explorer-heavy
ritualist-heavy -> analytics/validation-suite/analysis/ritualist-heavy
gatherer-heavy -> analytics/validation-suite/analysis/gatherer-heavy
mixed -> analytics/validation-suite/analysis/mixed
random-baseline -> analytics/validation-suite/analysis/random-baseline

SECTION 4: ROOT DISCOVERY VERDICT

PARTIAL

Rationale:
- A complete, scenario-mapped root is available (`analytics/validation-suite/analysis`).
- Rerun reports indicate newer run roots, but those referenced archive directories are not present in the checked-in filesystem, so the latest rerun dataset cannot be directly selected from disk.
