# Audit Report

## Scope
Audit focus requested:
- completeness
- commentation/documentation clarity
- configurability
- lag-friendliness/performance safety

## Checks run
1. `mvn -q -DskipTests package`
2. Manual code review of the plugin lifecycle, hot-path processors, and runtime configuration surface.

## Findings

### 1) Build remains blocked by Maven repository access (High, Environment)
- Packaging still fails before Java compilation due to inability to resolve Maven plugin artifacts from Maven Central (`403 Forbidden`).
- This is an environment/network artifact resolution issue, not a source syntax failure in reviewed files.

### 2) Completeness gaps (Medium)
- `plugin.yml` declares a broad command/permission surface not represented by command executor code in current sources.
- Plugin lifecycle had no explicit `onDisable` shutdown call path, risking future leaks if async/scheduled work is added.

### 3) Configurability gaps (Medium)
- Core balancing values were hardcoded in multiple engines (combat thresholds, evolution gates, drift formula, awakening gates).
- Operators had no runtime tuning point except source edits/rebuild.

### 4) Lag-friendliness concerns (Medium)
- Action bar updates occurred on every relevant event, creating avoidable spam in high-frequency combat.

### 5) Commentation and maintainability (Low)
- Critical orchestration code lacked explicit hot-path intent/comments.

## Remediations implemented in this pass

1. Added central runtime settings loader (`RuntimeSettings`) backed by `config.yml`.
   - Consolidates balancing values for combat, evolution, drift, awakening, and lore update cadence.

2. Added default plugin configuration file with commented tuning options (`src/main/resources/config.yml`).
   - Improves operational configurability without recompiling.

3. Wired startup lifecycle to load persisted config and runtime settings.
   - `onEnable`: save/load config snapshot.
   - `onDisable`: explicit engine shutdown.

4. Updated progression engines to consume configurable thresholds.
   - `ArtifactProcessor`, `EvolutionEngine`, `DriftEngine`, `AwakeningEngine` now read from runtime settings.

5. Added lore/action-bar throttling.
   - `LoreEngine` now tracks last update per-player and enforces `lore.min-update-interval-ms` to reduce event-spam overhead.

6. Improved code comments in hot-path orchestration.
   - Clarifies performance intent and why implementations avoid unnecessary overhead.

## Remaining recommendations

1. Implement or remove declared commands in `plugin.yml` to align behavior and metadata.
2. Add lightweight unit tests for deterministic logic:
   - evolution tier thresholds
   - drift clamp behavior
   - awakening gate precedence
3. Add optional periodic cleanup of in-memory player maps for long-lived servers (e.g., on quit events) to avoid stale entries.
4. Once Maven access is restored, run full static checks and integration smoke tests.

## Conclusion
This audit pass improved configurability and runtime efficiency without expanding hot-path complexity. The primary blocker for full validation remains external Maven artifact access.


## Follow-up wiring pass
Additional loose/unwired code remediation completed:
- Wired `/obtuseloot` command to a concrete executor (`info`, `help`, `inspect`) so plugin.yml command metadata has runtime handling.
- Wired the large static name pools (`Prefixes`, `Suffixes`, `Generic`) through a deterministic `ArtifactNameGenerator` used at artifact creation time.
- Added player quit cleanup listener to remove in-memory artifact/reputation/lore-throttle state and avoid stale map growth.
- Removed unused `SoulData` model that had no call sites.

## 2026-03-08 full audit pass

### Additional checks run
1. `./scripts/diagnose-maven-access.sh`
2. `mvn -B -ntp clean test`
3. Workflow inventory check: `git ls-files | rg '^\.github/'`

### Additional findings

1. **Critical CI governance gap (High)**  
   The repository had no `.github/workflows` files tracked, so there was no automated enforcement for build/test/publish policy.

2. **Build policy mismatch (Medium)**  
   README documented a `maven-publish` workflow, but no corresponding workflow file existed.

3. **Network-constrained build reliability (Medium, Environment)**  
   Direct Maven execution still fails in this environment because Maven Central access returns `403`, so workflows should explicitly support mirror-based execution.

### Remediation implemented in this pass

1. Added `.github/workflows/ci.yml` to enforce checkout + JDK setup + Maven access diagnosis + test execution on push/PR.
2. Added `.github/workflows/maven-publish.yml` to enforce publish pipeline on release/workflow dispatch.
3. Both workflows now support restricted-network mirrors via existing `MAVEN_MIRROR_URL` / proxy secret inputs and `scripts/mvn-via-mirror.sh`.

### Recommended next steps

1. Configure repository secrets for mirror/proxy if runners cannot access Maven Central directly.
2. Add branch protection requiring the `ci` workflow to pass before merge.
3. Add signing/publishing credentials in GitHub Environments with least privilege.
