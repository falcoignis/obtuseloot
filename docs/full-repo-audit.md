# Full Repository Audit (Pass 1)

## 1. Executive summary
- ObtuseLoot has strong modular separation by gameplay domain (abilities, evolution, ecosystem, persistence, simulation) and a healthy test baseline for core deterministic logic.
- The repository is broadly complete for a pre-release plugin, but there are a few integration/documentation drifts and at least one stale CI artifact path.
- Simulation and analytics coverage is unusually broad for a Bukkit plugin; however, parts of the methodology are single-run summary heavy and should be treated as directional unless cross-checked against the included multi-run artifacts.

## 2. Major strengths
- Clear subsystem decomposition under `src/main/java/obtuseloot/*` (commands, persistence, evolution, ecosystem, simulation, analytics).
- Runtime settings are centralized and actually consumed in hot paths (combat/reputation/evolution/drift/scheduler/naming), reducing hidden constants.
- Multi-backend persistence abstraction is present (`yaml`, `sqlite`, `mysql`) with migration helpers and stores split by concern.
- Existing unit tests cover genome/evolution/ecosystem behavior in `src/test/java`.
- CI/workflows include build, release, benchmark, dashboard regeneration, world-lab, and open-endedness runs.

## 3. Major weaknesses
- Workflow/version coupling drift existed: nightly upload referenced an old fixed JAR filename (`0.9.5`) while project/plugin are `0.9.6`.
- Some documentation/help surfaces are out of sync with currently implemented command branches (example: `ecosystem environment` exists in command handling but is absent in `plugin.yml` usage block).
- A few code artifacts appear abandoned/placeholding (notably `BalanceSignal` record) with no references.
- Build helper references a mirror helper script that is not present (`scripts/mvn-via-mirror.sh`), creating an incomplete fallback path.

## 4. Completeness findings
- **Config usage completeness:** runtime keys in `config.yml` are wired through `RuntimeSettings` and referenced at call sites (combat windows, thresholds, drift probabilities, autosave interval, naming controls).
- **Command completeness:** core admin/debug ecosystems are implemented and tab-completed; command surface is large and mostly functional.
- **Partial implementation concern:** `scripts/build.sh` advertises an offline/mirror fallback to `scripts/mvn-via-mirror.sh`, but that script does not exist, so fallback mode is incomplete.
- **Plugin command usage text drift:** `plugin.yml` usage omits at least one implemented subcommand path (`ecosystem environment`).

## 5. Dead/abandoned code findings
- `src/main/java/obtuseloot/ecosystem/BalanceSignal.java` is currently unreferenced by other Java sources (candidate abandoned scaffold).
- No broad dead-package area detected; most classes are integrated via constructors, commands, or simulation entry points.
- No committed `target/` or `.jar` build outputs found in tracked files.

## 6. Commentation/documentation findings
- High-level README and docs set is extensive; release and newcomer docs exist.
- Several docs are snapshot/version specific and may age quickly (release docs in `releases/` and analytics markdown snapshots).
- Command usage in `plugin.yml` is not fully synchronized with current command handlers.
- In-code comments are present in scripts and POM, but some complex analytics/simulation methods rely on implicit behavior with limited algorithmic commentary.

## 7. Simulation/analytics findings
- Simulation pipeline is substantive (world harness, open-endedness runner, metrics collector, dashboard generation, heatmaps).
- Metrics include distribution-level and convergence/collapse indicators; this is stronger than simple toy reporting.
- Caveat: some report conclusions depend on single-run summaries and should continue to be validated against multi-run outputs (`multirun-*` artifacts) before balance decisions.
- Analytics repository already contains generated artifacts; useful for review but prone to staleness if not continuously regenerated.

## 8. Repo/workflow hygiene findings
- **Fixed:** stale nightly artifact path updated from version-pinned JAR name to wildcard (safer across version bumps).
- Workflow coverage is broad and generally coherent.
- Potential hygiene issue: large generated analytics/document artifacts are committed; acceptable if intentional, but increases review noise and stale-data risk.
- `scripts/build.sh` references a missing mirror helper script, reducing resilience in restricted-network environments.

## 9. Small safe fixes applied
1. Updated nightly workflow artifact upload path from `target/ObtuseLoot-0.9.5.jar` to `target/ObtuseLoot-*.jar` in `.github/workflows/nightly-build.yml`.

## 10. Larger issues not fixed
- Did **not** remove unreferenced `BalanceSignal` because risk/intent is ambiguous (could be near-future integration scaffold).
- Did **not** rework command/help/doc synchronization broadly; only audited and reported drift.
- Did **not** redesign simulation methodology or rebalance systems (out of scope and higher risk).
- Did **not** add missing `scripts/mvn-via-mirror.sh`; this needs product decision on desired mirror behavior and security posture.
