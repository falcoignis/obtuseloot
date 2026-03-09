# Repository Hygiene Audit (v0.9.5 pass)

## 1) Cleanup performed
- Removed committed Maven build output under `target/` from version control.
- Added a repository-level `.gitignore` with explicit Maven/build/IDE/OS/temp analytics exclusions.
- Added `releases/v0.9.5/` release documentation set.
- Performed a conservative organization pass on analytics helper docs.

## 2) Removed from version control
- `target/` generated Maven output directory (compiled classes and Maven status files).

## 3) .gitignore rules added/confirmed
Added:
- `target/`
- `dist/`
- `artifacts/`
- `.idea/`
- `*.iml`
- `.vscode/`
- `.DS_Store`
- `Thumbs.db`
- `analytics/**/temp/`
- `analytics/**/scratch/`

## 4) Release docs added for v0.9.5
- `releases/v0.9.5/CHANGELOG.md`
- `releases/v0.9.5/QA_NOTES.md`
- `releases/v0.9.5/BUILD_INFO.md`

## 5) Workflow output hygiene fixes
- Updated nightly artifact upload JAR path to `target/ObtuseLoot-0.9.5.jar`.
- Updated release asset glob to `target/ObtuseLoot-*.jar` to match Maven final name.
- Confirmed CI/nightly build into runtime `target/` and upload binaries as artifacts, not commits.

## 6) Analytics/release clutter findings
- Analytics root contained review/helper docs mixed with generated reports.
- `analytics/world-lab/world-sim-report.md` and `large-world-sim-report.md` currently duplicate content and naming intent is ambiguous.
- Release folders for v0.9.2 and v0.9.3 were intact and left unchanged.

## 7) Reorganization performed
- Moved helper/review docs into `analytics/review/`:
  - `PHASE_LEDGER.md`
  - `procedural-generator-audit.md`
- Updated `codex/run_internal_pipeline.py` to continue writing `procedural-generator-audit.md` into `analytics/review/`.

## 8) Intentionally preserved ambiguities
- Preserved both `world-sim-report.md` and `large-world-sim-report.md` for historical continuity; potential dedupe can be decided in a focused future pass.
- Preserved existing analytics report naming outside minimal organization changes to avoid breaking references.

## 9) Follow-up suggestions
- Consider a small world-lab naming normalization pass (`primary` vs `large` naming) with reference updates.
- Consider moving additional long-lived root-level analytics summaries into domain folders (`meta/` or `review/`) once downstream links are inventoried.
