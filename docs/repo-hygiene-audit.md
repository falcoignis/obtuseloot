# Repository Hygiene Audit (v0.9.5)

## Completed in this pass
- Confirmed no tracked files remain under `target/` and retained `target/` as Maven runtime output only.
- Confirmed `.gitignore` covers Maven output, local artifact directories, IDE/OS files, and analytics temp/scratch paths.
- Confirmed release documentation exists under `releases/v0.9.5/` (`CHANGELOG.md`, `QA_NOTES.md`, `BUILD_INFO.md`).
- Added `analytics/README.md` as a light organization aid describing expected subfolders and preserving historical root files.
- Reviewed workflow output handling to ensure binaries are produced in CI runtime `target/` and uploaded as artifacts/release assets instead of committed files.
- Applied README/build-doc consistency fixes for Maven command wording and local JAR output expectations.

## Intentionally preserved
- Existing historical analytics report filenames/content (including overlapping world-lab report names) were left unchanged to avoid breaking references in scripts or docs.
- Existing release folders (`v0.9.2`, `v0.9.3`, `v0.9.5`) were kept as-is aside from v0.9.5 consistency edits.

## Minor follow-up recommendations
- If desired later, normalize world-lab report naming (`world-sim` vs `large-world-sim`) in a dedicated pass with link/script updates.
- Add `releases/nightly/README.md` only when nightly release-note text files are introduced.
